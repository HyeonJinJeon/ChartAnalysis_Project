package com.chartanalysis.kafka;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class StockPriceMessage {
    private String symbol;
    private BigDecimal price;       // current / close price
    private BigDecimal changeRate;
    private Long volume;
    private LocalDateTime timestamp;

    // OHLC fields (optional — if null, consumer falls back to price)
    private BigDecimal openPrice;
    private BigDecimal highPrice;
    private BigDecimal lowPrice;

    /** Convenience constructor matching original 5-arg signature. */
    public StockPriceMessage(String symbol, BigDecimal price, BigDecimal changeRate,
                             Long volume, LocalDateTime timestamp) {
        this.symbol = symbol;
        this.price = price;
        this.changeRate = changeRate;
        this.volume = volume;
        this.timestamp = timestamp;
    }
}
