package com.chartanalysis.domain.trade.dto;

import com.chartanalysis.domain.trade.Trade;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
public class TradeResponse {
    private final Long tradeId;
    private final String symbol;
    private final String stockName;
    private final String market;
    private final String type;
    private final Integer quantity;
    private final BigDecimal price;
    private final BigDecimal totalAmount;
    private final BigDecimal availableCash;
    private final LocalDateTime tradedAt;

    public TradeResponse(Trade trade, BigDecimal availableCash) {
        this.tradeId = trade.getId();
        this.symbol = trade.getStock().getSymbol();
        this.stockName = trade.getStock().getName();
        this.market = trade.getStock().getMarket().name();
        this.type = trade.getType().name();
        this.quantity = trade.getQuantity();
        this.price = trade.getPrice();
        this.totalAmount = trade.getTotalAmount();
        this.availableCash = availableCash;
        this.tradedAt = trade.getTradedAt();
    }
}
