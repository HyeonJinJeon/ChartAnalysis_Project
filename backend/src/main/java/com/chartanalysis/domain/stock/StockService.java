package com.chartanalysis.domain.stock;

import com.chartanalysis.domain.stock.dto.CandleResponse;
import com.chartanalysis.domain.stock.dto.MarketIndexResponse;
import com.chartanalysis.domain.stock.dto.StockResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class StockService {

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
}
