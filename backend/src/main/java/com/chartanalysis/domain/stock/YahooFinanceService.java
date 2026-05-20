package com.chartanalysis.domain.stock;

import com.chartanalysis.domain.stock.dto.MarketIndexResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
public class YahooFinanceService {

    private static final String YAHOO_CHART_URL =
            "https://query1.finance.yahoo.com/v8/finance/chart/{symbol}?interval={interval}&range={range}";

    private final RestTemplate restTemplate = new RestTemplate();

    /**
     * Returns the Yahoo Finance symbol for a given DB symbol and market.
     * KOSPI stocks get .KS suffix, KOSDAQ get .KQ suffix.
     * NASDAQ / index symbols are returned as-is.
     */
    public String toYahooSymbol(String dbSymbol, Market market) {
        if (market == Market.KOSPI) return dbSymbol + ".KS";
        if (market == Market.KOSDAQ) return dbSymbol + ".KQ";
        return dbSymbol;
    }

    /**
     * Maps an interval string to the appropriate Yahoo Finance range parameter.
     */
    private String rangeForInterval(String interval) {
        return switch (interval) {
            case "1m"  -> "1d";
            case "1d"  -> "1y";
            case "1wk" -> "2y";
            case "1mo" -> "5y";
            default    -> "1d";
        };
    }

    /**
     * Fetches historical candles from Yahoo Finance for the given symbol and interval.
     *
     * @param yahooSymbol the symbol as Yahoo Finance expects it (e.g. "005930.KS", "AAPL", "^KS11")
     * @param interval    candle interval: "1m", "1d", "1wk", "1mo"
     * @return list of StockPrice entities (not yet persisted)
     */
    @SuppressWarnings("unchecked")
    public List<StockPrice> fetchHistoricalCandles(String yahooSymbol, String interval, Stock stock) {
        List<StockPrice> result = new ArrayList<>();
        String range = rangeForInterval(interval);

        HttpHeaders headers = new HttpHeaders();
        headers.set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<Map> response = restTemplate.exchange(
                    YAHOO_CHART_URL, HttpMethod.GET, entity, Map.class,
                    Map.of("symbol", yahooSymbol, "interval", interval, "range", range)
            );

            if (response.getBody() == null) return result;

            Map<?, ?> chart = (Map<?, ?>) response.getBody().get("chart");
            if (chart == null) return result;

            List<?> resultList = (List<?>) chart.get("result");
            if (resultList == null || resultList.isEmpty()) return result;

            Map<?, ?> data = (Map<?, ?>) resultList.get(0);
            List<?> timestamps = (List<?>) data.get("timestamp");
            Map<?, ?> indicators = (Map<?, ?>) data.get("indicators");
            if (timestamps == null || indicators == null) return result;

            List<?> quoteList = (List<?>) indicators.get("quote");
            if (quoteList == null || quoteList.isEmpty()) return result;

            Map<?, ?> quote = (Map<?, ?>) quoteList.get(0);
            List<?> opens   = (List<?>) quote.get("open");
            List<?> highs   = (List<?>) quote.get("high");
            List<?> lows    = (List<?>) quote.get("low");
            List<?> closes  = (List<?>) quote.get("close");
            List<?> volumes = (List<?>) quote.get("volume");

            for (int i = 0; i < timestamps.size(); i++) {
                try {
                    Number ts    = (Number) timestamps.get(i);
                    Number close = closes  != null && i < closes.size()  ? (Number) closes.get(i)  : null;
                    Number open  = opens   != null && i < opens.size()   ? (Number) opens.get(i)   : close;
                    Number high  = highs   != null && i < highs.size()   ? (Number) highs.get(i)   : close;
                    Number low   = lows    != null && i < lows.size()    ? (Number) lows.get(i)    : close;
                    Number vol   = volumes != null && i < volumes.size() ? (Number) volumes.get(i) : null;

                    if (ts == null || close == null) continue;

                    LocalDateTime time = LocalDateTime.ofInstant(
                            Instant.ofEpochSecond(ts.longValue()), ZoneId.systemDefault());

                    result.add(StockPrice.builder()
                            .stock(stock)
                            .openPrice(toBd(open != null ? open : close))
                            .highPrice(toBd(high != null ? high : close))
                            .lowPrice(toBd(low  != null ? low  : close))
                            .closePrice(toBd(close))
                            .volume(vol != null ? vol.longValue() : 0L)
                            .timestamp(time)
                            .interval(interval)
                            .build());
                } catch (Exception e) {
                    log.debug("Skipping candle at index {}: {}", i, e.getMessage());
                }
            }
        } catch (Exception e) {
            log.warn("Failed to fetch Yahoo Finance candles for {}: {}", yahooSymbol, e.getMessage());
        }

        return result;
    }

    /**
     * Fetches current values for the major market indices.
     */
    @SuppressWarnings("unchecked")
    public List<MarketIndexResponse> fetchMarketIndices() {
        // symbol -> display name
        List<String[]> indices = List.of(
                new String[]{"^KS11",  "KOSPI"},
                new String[]{"^KQ11",  "KOSDAQ"},
                new String[]{"^GSPC",  "S&P500"},
                new String[]{"^IXIC",  "NASDAQ"},
                new String[]{"^NDX",   "나스닥100 선물"}
        );

        HttpHeaders headers = new HttpHeaders();
        headers.set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        List<MarketIndexResponse> result = new ArrayList<>();

        for (String[] entry : indices) {
            String symbol = entry[0];
            String name   = entry[1];
            try {
                ResponseEntity<Map> response = restTemplate.exchange(
                        YAHOO_CHART_URL, HttpMethod.GET, entity, Map.class,
                        Map.of("symbol", symbol, "interval", "1d", "range", "1d")
                );
                if (response.getBody() == null) continue;

                Map<?, ?> chart = (Map<?, ?>) response.getBody().get("chart");
                if (chart == null) continue;
                List<?> resultList = (List<?>) chart.get("result");
                if (resultList == null || resultList.isEmpty()) continue;

                Map<?, ?> data = (Map<?, ?>) resultList.get(0);
                Map<?, ?> meta = (Map<?, ?>) data.get("meta");
                if (meta == null) continue;

                Number price = (Number) meta.get("regularMarketPrice");
                Number prev  = (Number) meta.get("chartPreviousClose");
                if (price == null) continue;

                double currentValue = price.doubleValue();
                double prevClose    = prev != null ? prev.doubleValue() : currentValue;
                double change       = currentValue - prevClose;
                double changeRate   = prevClose != 0 ? (change / prevClose * 100) : 0;

                result.add(new MarketIndexResponse(name,
                        round(currentValue, 2),
                        round(change, 2),
                        round(changeRate, 2)));
            } catch (Exception e) {
                log.warn("Failed to fetch index {}: {}", symbol, e.getMessage());
            }
        }

        return result;
    }

    private BigDecimal toBd(Number n) {
        return BigDecimal.valueOf(n.doubleValue()).setScale(2, RoundingMode.HALF_UP);
    }

    private double round(double v, int scale) {
        return BigDecimal.valueOf(v).setScale(scale, RoundingMode.HALF_UP).doubleValue();
    }
}
