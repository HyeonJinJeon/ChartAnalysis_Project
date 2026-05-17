package com.chartanalysis.domain.portfolio.dto;

import com.chartanalysis.domain.portfolio.Holding;
import lombok.Getter;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Getter
public class HoldingResponse {
    private final String symbol;
    private final String name;
    private final Integer quantity;
    private final BigDecimal avgPrice;
    private final BigDecimal currentPrice;
    private final BigDecimal currentValue;
    private final BigDecimal profitLoss;
    private final BigDecimal profitLossRate;

    public HoldingResponse(Holding holding) {
        this.symbol = holding.getStock().getSymbol();
        this.name = holding.getStock().getName();
        this.quantity = holding.getQuantity();
        this.avgPrice = holding.getAvgPrice();
        this.currentPrice = holding.getStock().getCurrentPrice();
        this.currentValue = currentPrice.multiply(BigDecimal.valueOf(quantity));
        BigDecimal invested = avgPrice.multiply(BigDecimal.valueOf(quantity));
        this.profitLoss = currentValue.subtract(invested);
        this.profitLossRate = invested.compareTo(BigDecimal.ZERO) > 0
                ? profitLoss.divide(invested, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100))
                : BigDecimal.ZERO;
    }
}
