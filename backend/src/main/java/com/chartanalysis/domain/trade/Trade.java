package com.chartanalysis.domain.trade;

import com.chartanalysis.domain.stock.Stock;
import com.chartanalysis.domain.user.User;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "trades")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Trade {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "stock_id", nullable = false)
    private Stock stock;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TradeType type;

    @Column(nullable = false)
    private Integer quantity;

    @Column(precision = 20, scale = 2, nullable = false)
    private BigDecimal price;

    @Column(precision = 20, scale = 2, nullable = false)
    private BigDecimal totalAmount;

    @Column(nullable = false)
    private LocalDateTime tradedAt;

    @PrePersist
    protected void onCreate() {
        tradedAt = LocalDateTime.now();
    }
}
