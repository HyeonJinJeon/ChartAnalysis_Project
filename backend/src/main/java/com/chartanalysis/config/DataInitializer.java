package com.chartanalysis.config;

import com.chartanalysis.domain.stock.Market;
import com.chartanalysis.domain.stock.Stock;
import com.chartanalysis.domain.stock.StockRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final StockRepository stockRepository;

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

        stockRepository.saveAll(stocks);
        log.info("Seeded {} stocks. Candle data will be fetched from Yahoo Finance on first chart view.", stocks.size());
    }
}
