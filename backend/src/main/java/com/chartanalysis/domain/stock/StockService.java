package com.chartanalysis.domain.stock;

import com.chartanalysis.domain.stock.dto.CandleResponse;
import com.chartanalysis.domain.stock.dto.MarketIndexResponse;
import com.chartanalysis.domain.stock.dto.StockInfoResponse;
import com.chartanalysis.domain.stock.dto.StockNewsItem;
import com.chartanalysis.domain.stock.dto.StockResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockService {

    private static final String FINNHUB_API_KEY = "d84qfqpr01qutij9qhagd84qfqpr01qutij9qhb0";
    private static final String FINNHUB_COMPANY_NEWS_URL =
            "https://finnhub.io/api/v1/company-news?symbol={symbol}&from={from}&to={to}&token={token}";
    private static final String FINNHUB_GENERAL_NEWS_URL =
            "https://finnhub.io/api/v1/news?category=general&token={token}";

    private final RestTemplate restTemplate = new RestTemplate();

    private final StockRepository stockRepository;
    private final StockPriceRepository stockPriceRepository;
    private final YahooFinanceService yahooFinanceService;

    public List<StockResponse> getAllStocks() {
        return stockRepository.findAllByOrderByVolumeDesc().stream()
                .map(StockResponse::new)
                .collect(Collectors.toList());
    }

    public StockResponse getStock(String symbol) {
        Stock stock = stockRepository.findBySymbol(symbol)
                .orElseThrow(() -> new IllegalArgumentException("종목을 찾을 수 없습니다: " + symbol));
        return new StockResponse(stock);
    }

    @Transactional
    public List<CandleResponse> getCandles(String symbol, String interval, int limit) {
        Stock stock = stockRepository.findBySymbol(symbol)
                .orElseThrow(() -> new IllegalArgumentException("종목을 찾을 수 없습니다: " + symbol));

        List<StockPrice> fromDb = stockPriceRepository.findByStockAndIntervalOrderByTimestampDesc(
                stock, interval, PageRequest.of(0, limit));

        if (fromDb.size() < limit) {
            // Not enough data in DB — fetch from Yahoo Finance
            String yahooSymbol = yahooFinanceService.toYahooSymbol(symbol, stock.getMarket());
            log.info("Fetching historical candles from Yahoo Finance for {} (interval={})", yahooSymbol, interval);
            List<StockPrice> fetched = yahooFinanceService.fetchHistoricalCandles(yahooSymbol, interval, stock);
            if (!fetched.isEmpty()) {
                // Save only candles not already in DB (avoid duplicates by timestamp+interval)
                final List<StockPrice> existingSnapshot = fromDb;
                List<StockPrice> toSave = fetched.stream()
                        .filter(c -> existingSnapshot.stream()
                                .noneMatch(e -> e.getTimestamp().equals(c.getTimestamp())))
                        .collect(Collectors.toList());
                if (!toSave.isEmpty()) {
                    stockPriceRepository.saveAll(toSave);
                    log.info("Saved {} new candles for {} ({})", toSave.size(), symbol, interval);
                }
                // Re-query with the newly persisted data
                fromDb = stockPriceRepository.findByStockAndIntervalOrderByTimestampDesc(
                        stock, interval, PageRequest.of(0, limit));
            }
        }

        final List<StockPrice> result = fromDb;
        return result.stream()
                .sorted((a, b) -> a.getTimestamp().compareTo(b.getTimestamp()))
                .map(CandleResponse::new)
                .collect(Collectors.toList());
    }

    public List<MarketIndexResponse> getMarketIndices() {
        return yahooFinanceService.fetchMarketIndices();
    }

    public StockInfoResponse getStockInfo(String symbol) {
        Stock stock = stockRepository.findBySymbol(symbol)
                .orElseThrow(() -> new IllegalArgumentException("종목을 찾을 수 없습니다: " + symbol));

        LocalDateTime oneYearAgo = LocalDateTime.now().minusYears(1);
        BigDecimal weekHigh52 = stockPriceRepository.findMaxHighPriceSince(stock, oneYearAgo).orElse(null);
        BigDecimal weekLow52  = stockPriceRepository.findMinLowPriceSince(stock, oneYearAgo).orElse(null);

        return new StockInfoResponse(stock, weekHigh52, weekLow52);
    }

    @SuppressWarnings("unchecked")
    public List<StockNewsItem> getStockNews(String symbol) {
        Stock stock = stockRepository.findBySymbol(symbol)
                .orElseThrow(() -> new IllegalArgumentException("종목을 찾을 수 없습니다: " + symbol));

        HttpHeaders headers = new HttpHeaders();
        headers.set("User-Agent", "Mozilla/5.0");
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        boolean isNasdaq = stock.getMarket() == Market.NASDAQ;

        try {
            if (isNasdaq) {
                // Finnhub company-news for NASDAQ stocks
                String toDate   = java.time.LocalDate.now().toString();
                String fromDate = java.time.LocalDate.now().minusDays(7).toString();

                ResponseEntity<List> response = restTemplate.exchange(
                        FINNHUB_COMPANY_NEWS_URL, HttpMethod.GET, entity, List.class,
                        Map.of("symbol", symbol, "from", fromDate, "to", toDate, "token", FINNHUB_API_KEY)
                );

                if (response.getBody() == null) return Collections.emptyList();

                List<Map<String, Object>> articles = (List<Map<String, Object>>) response.getBody();
                return articles.stream()
                        .filter(a -> a.get("headline") != null)
                        .limit(5)
                        .map(a -> {
                            String headline    = safeStr(a.get("headline"));
                            String summary     = safeStr(a.get("summary"));
                            String url         = safeStr(a.get("url"));
                            String source      = safeStr(a.get("source"));
                            String publishedAt = formatUnixTimestamp(a.get("datetime"));
                            return new StockNewsItem(headline, summary, url, source, publishedAt);
                        })
                        .collect(Collectors.toList());
            } else {
                // Finnhub general market news for KR stocks
                ResponseEntity<List> response = restTemplate.exchange(
                        FINNHUB_GENERAL_NEWS_URL, HttpMethod.GET, entity, List.class,
                        Map.of("token", FINNHUB_API_KEY)
                );

                if (response.getBody() == null) return Collections.emptyList();

                List<Map<String, Object>> articles = (List<Map<String, Object>>) response.getBody();
                return articles.stream()
                        .filter(a -> a.get("headline") != null)
                        .limit(5)
                        .map(a -> {
                            String headline    = safeStr(a.get("headline"));
                            String summary     = safeStr(a.get("summary"));
                            String url         = safeStr(a.get("url"));
                            String source      = safeStr(a.get("source"));
                            String publishedAt = formatUnixTimestamp(a.get("datetime"));
                            return new StockNewsItem(headline, summary, url, source, publishedAt);
                        })
                        .collect(Collectors.toList());
            }
        } catch (Exception e) {
            log.warn("Failed to fetch news for {}: {}", symbol, e.getMessage());
            return Collections.emptyList();
        }
    }

    private String safeStr(Object obj) {
        return obj != null ? obj.toString() : "";
    }

    private String formatUnixTimestamp(Object obj) {
        if (obj == null) return "";
        try {
            long epochSeconds = ((Number) obj).longValue();
            LocalDateTime dt = LocalDateTime.ofInstant(Instant.ofEpochSecond(epochSeconds), ZoneId.systemDefault());
            return dt.format(DateTimeFormatter.ofPattern("MM.dd HH:mm"));
        } catch (Exception e) {
            return "";
        }
    }
}
