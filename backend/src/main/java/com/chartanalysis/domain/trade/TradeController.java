package com.chartanalysis.domain.trade;

import com.chartanalysis.domain.trade.dto.TradeRequest;
import com.chartanalysis.domain.trade.dto.TradeResponse;
import com.chartanalysis.domain.user.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/trade")
@RequiredArgsConstructor
public class TradeController {

    private final TradeService tradeService;

    @PostMapping("/execute")
    public ResponseEntity<TradeResponse> executeTrade(
            @AuthenticationPrincipal User user,
            @Valid @RequestBody TradeRequest request) {
        return ResponseEntity.ok(tradeService.executeTrade(user, request));
    }

    @GetMapping("/history")
    public ResponseEntity<List<TradeResponse>> getHistory(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(tradeService.getHistory(user));
    }
}
