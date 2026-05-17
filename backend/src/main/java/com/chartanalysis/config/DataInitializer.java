package com.chartanalysis.config;

import com.chartanalysis.domain.stock.Market;
import com.chartanalysis.domain.stock.Stock;
import com.chartanalysis.domain.stock.StockPrice;
import com.chartanalysis.domain.stock.StockPriceRepository;
import com.chartanalysis.domain.stock.StockRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final StockRepository stockRepository;
    private final StockPriceRepository stockPriceRepository;
    private final Random random = new Random();

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (stockRepository.count() > 0) {
            log.info("Stock data already exists, skipping DataInitializer.");
            return;
        }

        log.info("Seeding initial stock data...");

        List<Stock> stocks = List.of(
            // US stocks (NASDAQ) - prices in USD
            Stock.builder().symbol("AAPL").name("Apple Inc.").market(Market.NASDAQ)
                .currentPrice(BigDecimal.valueOf(185.0)).previousClose(BigDecimal.valueOf(185.0))
                .changeRate(BigDecimal.ZERO).volume(0L).build(),
            Stock.builder().symbol("TSLA").name("Tesla Inc.").market(Market.NASDAQ)
                .currentPrice(BigDecimal.valueOf(250.0)).previousClose(BigDecimal.valueOf(250.0))
                .changeRate(BigDecimal.ZERO).volume(0L).build(),
            Stock.builder().symbol("NVDA").name("NVIDIA Corp.").market(Market.NASDAQ)
                .currentPrice(BigDecimal.valueOf(875.0)).previousClose(BigDecimal.valueOf(875.0))
                .changeRate(BigDecimal.ZERO).volume(0L).build(),
            Stock.builder().symbol("MSFT").name("Microsoft Corp.").market(Market.NASDAQ)
                .currentPrice(BigDecimal.valueOf(415.0)).previousClose(BigDecimal.valueOf(415.0))
                .changeRate(BigDecimal.ZERO).volume(0L).build(),
            Stock.builder().symbol("AMZN").name("Amazon.com Inc.").market(Market.NASDAQ)
                .currentPrice(BigDecimal.valueOf(185.0)).previousClose(BigDecimal.valueOf(185.0))
                .changeRate(BigDecimal.ZERO).volume(0L).build(),
            Stock.builder().symbol("GOOGL").name("Alphabet Inc.").market(Market.NASDAQ)
                .currentPrice(BigDecimal.valueOf(175.0)).previousClose(BigDecimal.valueOf(175.0))
                .changeRate(BigDecimal.ZERO).volume(0L).build(),
            Stock.builder().symbol("META").name("Meta Platforms").market(Market.NASDAQ)
                .currentPrice(BigDecimal.valueOf(505.0)).previousClose(BigDecimal.valueOf(505.0))
                .changeRate(BigDecimal.ZERO).volume(0L).build(),

            // KR stocks - symbol = clean code (no .KS/.KQ), prices in KRW
            Stock.builder().symbol("005930").name("삼성전자").market(Market.KOSPI)
                .currentPrice(BigDecimal.valueOf(72000)).previousClose(BigDecimal.valueOf(72000))
                .changeRate(BigDecimal.ZERO).volume(0L).build(),
            Stock.builder().symbol("000660").name("SK하이닉스").market(Market.KOSPI)
                .currentPrice(BigDecimal.valueOf(185000)).previousClose(BigDecimal.valueOf(185000))
                .changeRate(BigDecimal.ZERO).volume(0L).build(),
            Stock.builder().symbol("035420").name("NAVER").market(Market.KOSPI)
                .currentPrice(BigDecimal.valueOf(195000)).previousClose(BigDecimal.valueOf(195000))
                .changeRate(BigDecimal.ZERO).volume(0L).build(),
            Stock.builder().symbol("035720").name("카카오").market(Market.KOSDAQ)
                .currentPrice(BigDecimal.valueOf(45000)).previousClose(BigDecimal.valueOf(45000))
                .changeRate(BigDecimal.ZERO).volume(0L).build(),
            Stock.builder().symbol("005380").name("현대차").market(Market.KOSPI)
                .currentPrice(BigDecimal.valueOf(235000)).previousClose(BigDecimal.valueOf(235000))
                .changeRate(BigDecimal.ZERO).volume(0L).build(),
            Stock.builder().symbol("051910").name("LG화학").market(Market.KOSPI)
                .currentPrice(BigDecimal.valueOf(285000)).previousClose(BigDecimal.valueOf(285000))
                .changeRate(BigDecimal.ZERO).volume(0L).build(),
            Stock.builder().symbol("006400").name("삼성SDI").market(Market.KOSPI)
                .currentPrice(BigDecimal.valueOf(295000)).previousClose(BigDecimal.valueOf(295000))
                .changeRate(BigDecimal.ZERO).volume(0L).build()
        );

        List<Stock> saved = stockRepository.saveAll(stocks);
        log.info("Seeded {} stocks.", saved.size());

        for (Stock stock : saved) {
            generateInitialCandles(stock, stock.getCurrentPrice());
        }
        log.info("Generated initial candle data for {} stocks.", saved.size());
    }

    private void generateInitialCandles(Stock stock, BigDecimal basePrice) {
        List<StockPrice> candles = new ArrayList<>();
        BigDecimal price = basePrice;
        LocalDateTime now = LocalDateTime.now().withMinute(0).withSecond(0).withNano(0);

        for (int i = 99; i >= 0; i--) {
            double change = (random.nextDouble() - 0.5) * 0.02;
            BigDecimal open = price;
            BigDecimal close = price.multiply(BigDecimal.valueOf(1 + change));
            BigDecimal high = open.max(close).multiply(BigDecimal.valueOf(1 + random.nextDouble() * 0.005));
            BigDecimal low = open.min(close).multiply(BigDecimal.valueOf(1 - random.nextDouble() * 0.005));

            candles.add(StockPrice.builder()
                    .stock(stock)
                    .openPrice(open.setScale(2, RoundingMode.HALF_UP))
                    .highPrice(high.setScale(2, RoundingMode.HALF_UP))
                    .lowPrice(low.setScale(2, RoundingMode.HALF_UP))
                    .closePrice(close.setScale(2, RoundingMode.HALF_UP))
                    .volume(random.nextLong(100000, 5000000))
                    .timestamp(now.minusMinutes(i))
                    .interval("1m")
                    .build());
            price = close;
        }
        stockPriceRepository.saveAll(candles);
    }
}
