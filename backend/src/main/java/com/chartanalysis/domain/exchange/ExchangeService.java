package com.chartanalysis.domain.exchange;

import com.chartanalysis.domain.portfolio.Portfolio;
import com.chartanalysis.domain.portfolio.PortfolioRepository;
import com.chartanalysis.domain.user.User;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExchangeService {

    private final ExchangeTransactionRepository exchangeTransactionRepository;
    private final PortfolioRepository portfolioRepository;
    private final StringRedisTemplate redisTemplate;

    private static final String REDIS_KEY = "exchange:USD:KRW";
    private static final String EXCHANGE_RATE_URL = "https://api.exchangerate-api.com/v4/latest/USD";
    private final ObjectMapper objectMapper = new ObjectMapper();

    public BigDecimal getCurrentRate() {
        try {
            String cached = redisTemplate.opsForValue().get(REDIS_KEY);
            if (cached != null && !cached.isEmpty()) {
                return new BigDecimal(cached);
            }
        } catch (Exception e) {
            log.warn("Redis unavailable for exchange rate: {}", e.getMessage());
        }

        // Fallback: fetch from API
        try {
            RestTemplate restTemplate = new RestTemplate();
            String responseStr = restTemplate.getForObject(EXCHANGE_RATE_URL, String.class);
            if (responseStr != null) {
                JsonNode root = objectMapper.readTree(responseStr);
                double rate = root.path("rates").path("KRW").asDouble();
                if (rate > 0) {
                    return BigDecimal.valueOf(rate);
                }
            }
        } catch (Exception e) {
            log.warn("Failed to fetch exchange rate from API: {}", e.getMessage());
        }

        // Default fallback rate
        return BigDecimal.valueOf(1380.0);
    }

    public LocalDateTime getRateUpdatedAt() {
        // Returns current time as approximation; a more precise version would store timestamp in Redis alongside rate
        return LocalDateTime.now();
    }

    @Transactional
    public ExchangeTransaction exchangeCurrency(User user, String fromCurrency, BigDecimal amount) {
        Portfolio portfolio = portfolioRepository.findByUser(user)
                .orElseThrow(() -> new IllegalArgumentException("포트폴리오를 찾을 수 없습니다."));

        BigDecimal rate = getCurrentRate();
        BigDecimal toAmount;
        String toCurrency;

        if ("KRW".equalsIgnoreCase(fromCurrency)) {
            // KRW -> USD
            if (portfolio.getAvailableCash().compareTo(amount) < 0) {
                throw new IllegalStateException("원화 잔액이 부족합니다. 현재 잔액: " + portfolio.getAvailableCash() + "원");
            }
            toAmount = amount.divide(rate, 6, RoundingMode.HALF_UP);
            toCurrency = "USD";
            portfolio.setAvailableCash(portfolio.getAvailableCash().subtract(amount));
            BigDecimal currentUsd = portfolio.getAvailableUsd() != null ? portfolio.getAvailableUsd() : BigDecimal.ZERO;
            portfolio.setAvailableUsd(currentUsd.add(toAmount));

        } else if ("USD".equalsIgnoreCase(fromCurrency)) {
            // USD -> KRW
            BigDecimal currentUsd = portfolio.getAvailableUsd() != null ? portfolio.getAvailableUsd() : BigDecimal.ZERO;
            if (currentUsd.compareTo(amount) < 0) {
                throw new IllegalStateException("달러 잔액이 부족합니다. 현재 잔액: $" + currentUsd);
            }
            toAmount = amount.multiply(rate).setScale(2, RoundingMode.HALF_UP);
            toCurrency = "KRW";
            portfolio.setAvailableUsd(currentUsd.subtract(amount));
            portfolio.setAvailableCash(portfolio.getAvailableCash().add(toAmount));

        } else {
            throw new IllegalArgumentException("지원하지 않는 통화입니다: " + fromCurrency);
        }

        portfolioRepository.save(portfolio);

        ExchangeTransaction transaction = ExchangeTransaction.builder()
                .user(user)
                .fromCurrency(fromCurrency.toUpperCase())
                .toCurrency(toCurrency)
                .fromAmount(amount)
                .toAmount(toAmount)
                .exchangeRate(rate)
                .build();

        return exchangeTransactionRepository.save(transaction);
    }
}
