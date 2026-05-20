package com.chartanalysis.domain.portfolio.dto;

import com.chartanalysis.domain.portfolio.Holding;
import com.chartanalysis.domain.stock.Market;
import lombok.Getter;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Getter
public class HoldingResponse {
    private final String symbol;
    private final String name;
    private final String market;
    private final Integer quantity;
    private final BigDecimal avgPrice;
    private final BigDecimal currentPrice;
    private final BigDecimal currentValue;
    private final BigDecimal profitLoss;
    private final BigDecimal profitLossRate;

    public HoldingResponse(Holding holding) {
        this.symbol = holding.getStock().getSymbol();
        this.name = holding.getStock().getName();
        this.market = holding.getStock().getMarket().name();
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
