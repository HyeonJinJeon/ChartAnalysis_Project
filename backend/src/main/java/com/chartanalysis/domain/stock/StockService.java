package com.chartanalysis.domain.stock;

import com.chartanalysis.domain.stock.dto.CandleResponse;
import com.chartanalysis.domain.stock.dto.StockResponse;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StockService {

    private final StockRepository stockRepository;
    private final StockPriceRepository stockPriceRepository;
    private final Random random = new Random();

    @PostConstruct
    @Transactional
    public void initializeStocks() {
        if (stockRepository.count() > 0) return;

        List<Object[]> stockData = List.of(
            new Object[]{"005930", "삼성전자", Market.KOSPI, 73400},
            new Object[]{"000660", "SK하이닉스", Market.KOSPI, 198000},
            new Object[]{"035420", "NAVER", Market.KOSPI, 182000},
            new Object[]{"035720", "카카오", Market.KOSPI, 42350},
            new Object[]{"051910", "LG화학", Market.KOSPI, 285000},
            new Object[]{"207940", "삼성바이오로직스", Market.KOSPI, 875000},
            new Object[]{"006400", "삼성SDI", Market.KOSPI, 168500},
            new Object[]{"005380", "현대차", Market.KOSPI, 198500},
            new Object[]{"068270", "셀트리온", Market.KOSDAQ, 172500},
            new Object[]{"247540", "에코프로비엠", Market.KOSDAQ, 108500},
            new Object[]{"AAPL", "Apple", Market.NASDAQ, 189},
            new Object[]{"TSLA", "Tesla", Market.NASDAQ, 248},
            new Object[]{"NVDA", "NVIDIA", Market.NASDAQ, 875},
            new Object[]{"MSFT", "Microsoft", Market.NASDAQ, 415},
            new Object[]{"AMZN", "Amazon", Market.NASDAQ, 186}
        );

        for (Object[] data : stockData) {
            BigDecimal basePrice = BigDecimal.valueOf((int) data[3]);
            Stock stock = Stock.builder()
                    .symbol((String) data[0])
                    .name((String) data[1])
                    .market((Market) data[2])
                    .currentPrice(basePrice)
                    .previousClose(basePrice)
                    .changeRate(BigDecimal.ZERO)
                    .volume(random.nextLong(1000000, 50000000))
                    .build();
            stockRepository.save(stock);

            generateInitialCandles(stock, basePrice);
        }
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
                    .openPrice(open.setScale(2, java.math.RoundingMode.HALF_UP))
                    .highPrice(high.setScale(2, java.math.RoundingMode.HALF_UP))
                    .lowPrice(low.setScale(2, java.math.RoundingMode.HALF_UP))
                    .closePrice(close.setScale(2, java.math.RoundingMode.HALF_UP))
                    .volume(random.nextLong(100000, 5000000))
                    .timestamp(now.minusMinutes(i))
                    .interval("1m")
                    .build());
            price = close;
        }
        stockPriceRepository.saveAll(candles);
    }

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

    public List<CandleResponse> getCandles(String symbol, String interval, int limit) {
        Stock stock = stockRepository.findBySymbol(symbol)
                .orElseThrow(() -> new IllegalArgumentException("종목을 찾을 수 없습니다: " + symbol));

        List<StockPrice> prices = stockPriceRepository.findByStockAndIntervalOrderByTimestampDesc(
                stock, interval, PageRequest.of(0, limit));

        return prices.stream()
                .sorted((a, b) -> a.getTimestamp().compareTo(b.getTimestamp()))
                .map(CandleResponse::new)
                .collect(Collectors.toList());
    }
}
