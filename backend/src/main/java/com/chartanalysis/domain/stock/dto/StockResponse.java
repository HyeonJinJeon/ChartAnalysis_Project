package com.chartanalysis.domain.stock.dto;

import com.chartanalysis.domain.stock.Stock;
import lombok.Getter;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Getter
public class StockResponse {
    private final Long id;
    private final String symbol;
    private final String name;
    private final String market;
    private final BigDecimal currentPrice;
    private final BigDecimal previousClose;
    private final BigDecimal changeAmount;
    private final BigDecimal changeRate;
    private final Long volume;

    public StockResponse(Stock stock) {
        this.id = stock.getId();
        this.symbol = stock.getSymbol();
        this.name = stock.getName();
        this.market = stock.getMarket().name();
        this.currentPrice = stock.getCurrentPrice();
        this.previousClose = stock.getPreviousClose();
        this.changeAmount = stock.getCurrentPrice() != null && stock.getPreviousClose() != null
                ? stock.getCurrentPrice().subtract(stock.getPreviousClose())
                : BigDecimal.ZERO;
        this.changeRate = stock.getChangeRate() != null
                ? stock.getChangeRate().setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        this.volume = stock.getVolume();
    }
}
