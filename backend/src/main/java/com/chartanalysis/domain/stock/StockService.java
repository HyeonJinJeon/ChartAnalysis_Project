package com.chartanalysis.domain.stock;

import com.chartanalysis.domain.stock.dto.CandleResponse;
import com.chartanalysis.domain.stock.dto.StockResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class StockService {

    private final StockRepository stockRepository;
    private final StockPriceRepository stockPriceRepository;

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
