package com.chartanalysis.kafka;

import com.chartanalysis.domain.stock.Stock;
import com.chartanalysis.domain.stock.StockPrice;
import com.chartanalysis.domain.stock.StockPriceRepository;
import com.chartanalysis.domain.stock.StockRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.temporal.ChronoUnit;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class StockDataConsumer {

    private final StockRepository stockRepository;
    private final StockPriceRepository stockPriceRepository;
    private final SimpMessagingTemplate messagingTemplate;

    @KafkaListener(topics = "stock-prices", groupId = "chart-analysis-group")
    @Transactional
    public void consume(StockPriceMessage message) {
        stockRepository.findBySymbol(message.getSymbol()).ifPresent(stock -> {
            BigDecimal prevClose = stock.getPreviousClose() != null ? stock.getPreviousClose() : message.getPrice();
            BigDecimal changeRate = message.getPrice()
                    .subtract(prevClose)
                    .divide(prevClose, 4, RoundingMode.HALF_UP)
                    .multiply(BigDecimal.valueOf(100));

            stock.setCurrentPrice(message.getPrice());
            stock.setChangeRate(changeRate);
            stock.setVolume(message.getVolume());
            stockRepository.save(stock);

            saveCandle(stock, message);

            messagingTemplate.convertAndSend(
                    "/topic/stocks/" + message.getSymbol(),
                    Map.of(
                            "symbol", message.getSymbol(),
                            "price", message.getPrice(),
                            "changeRate", changeRate.setScale(2, RoundingMode.HALF_UP),
                            "volume", message.getVolume(),
                            "timestamp", message.getTimestamp().toString()
                    )
            );
        });
    }

    private void saveCandle(Stock stock, StockPriceMessage message) {
        // Truncate to the current minute boundary for proper 1m OHLC aggregation
        java.time.LocalDateTime minuteTs = message.getTimestamp().truncatedTo(ChronoUnit.MINUTES);
        BigDecimal price = message.getPrice();

        stockPriceRepository.findByStockAndIntervalAndTimestamp(stock, "1m", minuteTs)
                .ifPresentOrElse(existing -> {
                    // Update OHLC: high/low may widen, close always moves to latest price
                    if (price.compareTo(existing.getHighPrice()) > 0) existing.setHighPrice(price);
                    if (price.compareTo(existing.getLowPrice()) < 0)  existing.setLowPrice(price);
                    existing.setClosePrice(price);
                    existing.setVolume(message.getVolume());
                    stockPriceRepository.save(existing);
                }, () -> {
                    // New candle: open=high=low=close=current price; wicks grow via subsequent updates
                    stockPriceRepository.save(StockPrice.builder()
                            .stock(stock)
                            .openPrice(price)
                            .highPrice(price)
                            .lowPrice(price)
                            .closePrice(price)
                            .volume(message.getVolume())
                            .timestamp(minuteTs)
                            .interval("1m")
                            .build());
                });
    }
}
