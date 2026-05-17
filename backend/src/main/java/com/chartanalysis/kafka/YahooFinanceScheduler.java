package com.chartanalysis.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class YahooFinanceScheduler {

    private final StockDataProducer producer;

    @Value("${yahoo.kr-symbols}")
    private String krSymbolsRaw;

    private static final String YAHOO_URL =
            "https://query1.finance.yahoo.com/v8/finance/chart/{symbol}?interval=1m&range=1d";

    @Scheduled(fixedDelay = 5000)
    public void fetchKoreanStockPrices() {
        String[] yahooSymbols = krSymbolsRaw.split(",");
        RestTemplate restTemplate = new RestTemplate();

        HttpHeaders headers = new HttpHeaders();
        headers.set("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36");
        HttpEntity<Void> entity = new HttpEntity<>(headers);

        for (String yahooSymbol : yahooSymbols) {
            yahooSymbol = yahooSymbol.trim();
            try {
                ResponseEntity<Map> response = restTemplate.exchange(
                        YAHOO_URL, HttpMethod.GET, entity, Map.class,
                        Map.of("symbol", yahooSymbol)
                );

                if (response.getBody() == null) continue;

                Map<?, ?> chart = (Map<?, ?>) response.getBody().get("chart");
                if (chart == null) continue;

                List<?> resultList = (List<?>) chart.get("result");
                if (resultList == null || resultList.isEmpty()) continue;

                Map<?, ?> result = (Map<?, ?>) resultList.get(0);
                Map<?, ?> meta = (Map<?, ?>) result.get("meta");
                if (meta == null) continue;

                Number regularMarketPrice  = (Number) meta.get("regularMarketPrice");
                Number regularMarketOpen   = (Number) meta.get("regularMarketOpen");
                Number regularMarketHigh   = (Number) meta.get("regularMarketDayHigh");
                Number regularMarketLow    = (Number) meta.get("regularMarketDayLow");
                Number regularMarketVolume = (Number) meta.get("regularMarketVolume");

                if (regularMarketPrice == null) continue;

                BigDecimal closePrice  = BigDecimal.valueOf(regularMarketPrice.doubleValue()).setScale(2, java.math.RoundingMode.HALF_UP);
                BigDecimal openPrice   = regularMarketOpen   != null ? BigDecimal.valueOf(regularMarketOpen.doubleValue()).setScale(2, java.math.RoundingMode.HALF_UP)   : closePrice;
                BigDecimal highPrice   = regularMarketHigh   != null ? BigDecimal.valueOf(regularMarketHigh.doubleValue()).setScale(2, java.math.RoundingMode.HALF_UP)   : closePrice;
                BigDecimal lowPrice    = regularMarketLow    != null ? BigDecimal.valueOf(regularMarketLow.doubleValue()).setScale(2, java.math.RoundingMode.HALF_UP)    : closePrice;
                long       volume      = regularMarketVolume != null ? regularMarketVolume.longValue() : 0L;

                // Strip .KS or .KQ suffix to get the clean DB symbol
                String cleanSymbol = yahooSymbol.replaceAll("\\.(KS|KQ)$", "");

                StockPriceMessage msg = new StockPriceMessage(
                        cleanSymbol, closePrice, BigDecimal.ZERO, volume, LocalDateTime.now()
                );
                msg.setOpenPrice(openPrice);
                msg.setHighPrice(highPrice);
                msg.setLowPrice(lowPrice);

                producer.send(msg);
                log.debug("Yahoo Finance: {} -> {}", cleanSymbol, closePrice);

            } catch (Exception e) {
                log.warn("Failed to fetch Yahoo Finance data for {}: {}", yahooSymbol, e.getMessage());
            }
        }
    }
}
