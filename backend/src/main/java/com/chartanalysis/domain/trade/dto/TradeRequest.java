package com.chartanalysis.domain.trade.dto;

import com.chartanalysis.domain.trade.TradeType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class TradeRequest {
    @NotBlank
    private String symbol;

    @NotNull
    @Min(value = 1, message = "최소 1주 이상 거래해야 합니다.")
    private Integer quantity;

    @NotNull
    private TradeType type;
}
