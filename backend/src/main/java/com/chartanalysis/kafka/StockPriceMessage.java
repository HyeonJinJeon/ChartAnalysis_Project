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
    private BigDecimal price;
    private BigDecimal changeRate;
    private Long volume;
    private LocalDateTime timestamp;
}
