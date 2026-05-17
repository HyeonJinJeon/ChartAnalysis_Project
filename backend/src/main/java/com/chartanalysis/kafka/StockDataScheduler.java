package com.chartanalysis.kafka;

import com.chartanalysis.domain.stock.StockRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.Random;

@Slf4j
@Component
@RequiredArgsConstructor
public class StockDataScheduler {

    private final StockRepository stockRepository;
    private final StockDataProducer producer;
    private final Random random = new Random();

    // Disabled: replaced by FinnhubWebSocketClient (US) and YahooFinanceScheduler (KR)
    // @Scheduled(fixedDelay = 2000)
    public void generateStockPrices() {
        stockRepository.findAll().forEach(stock -> {
            if (stock.getCurrentPrice() == null) return;

            double changePercent = (random.nextGaussian() * 0.003);
            BigDecimal newPrice = stock.getCurrentPrice()
                    .multiply(BigDecimal.valueOf(1 + changePercent))
                    .setScale(stock.getMarket().name().equals("NASDAQ") ? 2 : 0, RoundingMode.HALF_UP);

            if (newPrice.compareTo(BigDecimal.ONE) < 0) newPrice = BigDecimal.ONE;

            long volumeDelta = random.nextLong(10000, 500000);

            producer.send(new StockPriceMessage(
                    stock.getSymbol(),
                    newPrice,
                    BigDecimal.ZERO,
                    (stock.getVolume() != null ? stock.getVolume() : 0L) + volumeDelta,
                    LocalDateTime.now()
            ));
        });
    }
}
