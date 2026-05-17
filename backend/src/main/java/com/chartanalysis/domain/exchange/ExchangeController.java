package com.chartanalysis.domain.exchange;

import com.chartanalysis.domain.user.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.Map;

@RestController
@RequestMapping("/api/exchange")
@RequiredArgsConstructor
public class ExchangeController {

    private final ExchangeService exchangeService;

    @GetMapping("/rate")
    public ResponseEntity<Map<String, Object>> getCurrentRate() {
        BigDecimal rate = exchangeService.getCurrentRate();
        return ResponseEntity.ok(Map.of(
                "rate", rate,
                "updatedAt", exchangeService.getRateUpdatedAt().toString()
        ));
    }

    @PostMapping
    public ResponseEntity<Map<String, Object>> exchange(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody ExchangeRequest request) {
        ExchangeTransaction transaction = exchangeService.exchangeCurrency(
                user, request.getFromCurrency(), request.getAmount());

        return ResponseEntity.ok(Map.of(
                "fromCurrency", transaction.getFromCurrency(),
                "toCurrency", transaction.getToCurrency(),
                "fromAmount", transaction.getFromAmount(),
                "toAmount", transaction.getToAmount(),
                "exchangeRate", transaction.getExchangeRate(),
                "createdAt", transaction.getCreatedAt().toString()
        ));
    }
}
