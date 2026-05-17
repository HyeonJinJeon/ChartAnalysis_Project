package com.chartanalysis.domain.portfolio;

import com.chartanalysis.domain.portfolio.dto.PortfolioResponse;
import com.chartanalysis.domain.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/portfolio")
@RequiredArgsConstructor
public class PortfolioController {

    private final PortfolioService portfolioService;

    @GetMapping("/me")
    public ResponseEntity<PortfolioResponse> getMyPortfolio(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(portfolioService.getPortfolio(user.getId()));
    }
}
