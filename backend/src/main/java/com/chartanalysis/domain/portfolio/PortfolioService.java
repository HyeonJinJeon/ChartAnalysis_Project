package com.chartanalysis.domain.portfolio;

import com.chartanalysis.domain.portfolio.dto.HoldingResponse;
import com.chartanalysis.domain.portfolio.dto.PortfolioResponse;
import com.chartanalysis.domain.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class PortfolioService {

    private final PortfolioRepository portfolioRepository;
    private final HoldingRepository holdingRepository;
    private final UserRepository userRepository;

    @Value("${stock.seed-money}")
    private long seedMoney;

    @Transactional(readOnly = true)
    public PortfolioResponse getPortfolio(Long userId) {
        Portfolio portfolio = portfolioRepository.findByUserId(userId)
                .orElseThrow(() -> new IllegalArgumentException("포트폴리오를 찾을 수 없습니다."));

        List<HoldingResponse> holdings = holdingRepository.findByPortfolio(portfolio).stream()
                .map(HoldingResponse::new)
                .collect(Collectors.toList());

        BigDecimal currentValue = holdings.stream()
                .map(HoldingResponse::getCurrentValue)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalInvested = holdings.stream()
                .map(h -> h.getAvgPrice().multiply(BigDecimal.valueOf(h.getQuantity())))
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal totalProfitLoss = currentValue.subtract(totalInvested);
        BigDecimal totalProfitLossRate = totalInvested.compareTo(BigDecimal.ZERO) > 0
                ? totalProfitLoss.divide(totalInvested, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100))
                : BigDecimal.ZERO;

        BigDecimal availableUsd = portfolio.getAvailableUsd() != null
                ? portfolio.getAvailableUsd()
                : BigDecimal.ZERO;

        return new PortfolioResponse(
                portfolio.getAvailableCash(),
                availableUsd,
                totalInvested,
                currentValue,
                totalProfitLoss,
                totalProfitLossRate,
                holdings
        );
    }

    @Scheduled(cron = "0 0 0 1 * *")
    @Transactional
    public void distributeMonthlySeed() {
        log.info("월간 시드머니 지급 시작: {}원", seedMoney);
        portfolioRepository.findAll().forEach(portfolio -> {
            portfolio.setAvailableCash(portfolio.getAvailableCash().add(BigDecimal.valueOf(seedMoney)));
            portfolioRepository.save(portfolio);
        });
        log.info("월간 시드머니 지급 완료");
    }
}
