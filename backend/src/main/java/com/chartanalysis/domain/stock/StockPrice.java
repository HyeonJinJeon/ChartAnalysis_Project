package com.chartanalysis.domain.stock;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "stock_prices", indexes = {
        @Index(columnList = "stock_id, price_interval, price_timestamp DESC")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class StockPrice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stock_id", nullable = false)
    private Stock stock;

    @Column(precision = 20, scale = 2)
    private BigDecimal openPrice;

    @Column(precision = 20, scale = 2)
    private BigDecimal highPrice;

    @Column(precision = 20, scale = 2)
    private BigDecimal lowPrice;

    @Column(precision = 20, scale = 2)
    private BigDecimal closePrice;

    private Long volume;

    @Column(name = "price_timestamp", nullable = false)
    private LocalDateTime timestamp;

    @Column(name = "price_interval", nullable = false)
    private String interval;
}
