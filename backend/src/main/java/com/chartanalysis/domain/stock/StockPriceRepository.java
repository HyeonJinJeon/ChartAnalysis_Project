package com.chartanalysis.domain.stock;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface StockPriceRepository extends JpaRepository<StockPrice, Long> {
    List<StockPrice> findByStockAndIntervalOrderByTimestampDesc(Stock stock, String interval, Pageable pageable);

    @Query("SELECT MAX(sp.highPrice) FROM StockPrice sp WHERE sp.stock = :stock AND sp.interval = '1d' AND sp.timestamp >= :since")
    Optional<BigDecimal> findMaxHighPriceSince(@Param("stock") Stock stock, @Param("since") LocalDateTime since);

    @Query("SELECT MIN(sp.lowPrice) FROM StockPrice sp WHERE sp.stock = :stock AND sp.interval = '1d' AND sp.timestamp >= :since")
    Optional<BigDecimal> findMinLowPriceSince(@Param("stock") Stock stock, @Param("since") LocalDateTime since);
}
