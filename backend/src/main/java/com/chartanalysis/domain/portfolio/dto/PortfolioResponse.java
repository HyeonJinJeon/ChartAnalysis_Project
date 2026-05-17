package com.chartanalysis.domain.portfolio.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@AllArgsConstructor
public class PortfolioResponse {
    private final BigDecimal availableCash;
    private final BigDecimal availableUsd;
    private final BigDecimal totalInvested;
    private final BigDecimal currentValue;
    private final BigDecimal totalProfitLoss;
    private final BigDecimal totalProfitLossRate;
    private final List<HoldingResponse> holdings;
}
