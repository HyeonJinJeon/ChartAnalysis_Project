package com.chartanalysis.domain.stock;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StockPriceRepository extends JpaRepository<StockPrice, Long> {
    List<StockPrice> findByStockAndIntervalOrderByTimestampDesc(Stock stock, String interval, Pageable pageable);
}
