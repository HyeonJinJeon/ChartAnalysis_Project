package com.chartanalysis.domain.stock.dto;

import com.chartanalysis.domain.stock.StockPrice;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
public class CandleResponse {
    private final LocalDateTime timestamp;
    private final BigDecimal open;
    private final BigDecimal high;
    private final BigDecimal low;
    private final BigDecimal close;
    private final Long volume;

    public CandleResponse(StockPrice price) {
        this.timestamp = price.getTimestamp();
        this.open = price.getOpenPrice();
        this.high = price.getHighPrice();
        this.low = price.getLowPrice();
        this.close = price.getClosePrice();
        this.volume = price.getVolume();
    }
}
