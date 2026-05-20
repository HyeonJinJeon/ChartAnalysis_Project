package com.chartanalysis.domain.stock.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class MarketIndexResponse {
    private final String name;
    private final double value;
    private final double change;
    private final double changeRate;
}
