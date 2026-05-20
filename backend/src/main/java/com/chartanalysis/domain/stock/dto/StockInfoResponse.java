package com.chartanalysis.domain.stock.dto;

import com.chartanalysis.domain.stock.Stock;
import lombok.Getter;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Getter
public class StockInfoResponse {
    private final String symbol;
    private final String name;
    private final String market;
    private final BigDecimal currentPrice;
    private final BigDecimal previousClose;
    private final BigDecimal changeRate;
    private final Long volume;
    private final BigDecimal weekHigh52;
    private final BigDecimal weekLow52;

    public StockInfoResponse(Stock stock, BigDecimal weekHigh52, BigDecimal weekLow52) {
        this.symbol = stock.getSymbol();
        this.name = stock.getName();
        this.market = stock.getMarket().name();
        this.currentPrice = stock.getCurrentPrice();
        this.previousClose = stock.getPreviousClose();
        this.changeRate = stock.getChangeRate() != null
                ? stock.getChangeRate().setScale(2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;
        this.volume = stock.getVolume();
        this.weekHigh52 = weekHigh52;
        this.weekLow52 = weekLow52;
    }
}
