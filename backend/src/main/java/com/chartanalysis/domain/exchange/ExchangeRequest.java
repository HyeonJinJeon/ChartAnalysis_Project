package com.chartanalysis.domain.exchange;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class ExchangeRequest {

    @NotBlank
    private String fromCurrency;

    @NotNull
    @DecimalMin(value = "0.01", message = "환전 금액은 0보다 커야 합니다.")
    private BigDecimal amount;
}
