package com.chartanalysis.kafka;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class ExchangeRateScheduler {

    private final StringRedisTemplate redisTemplate;
    private final SimpMessagingTemplate messagingTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String EXCHANGE_RATE_URL = "https://api.exchangerate-api.com/v4/latest/USD";
    private static final String REDIS_KEY = "exchange:USD:KRW";

    @Scheduled(fixedDelay = 30000)
    public void fetchExchangeRate() {
        try {
            RestTemplate restTemplate = new RestTemplate();
            String responseStr = restTemplate.getForObject(EXCHANGE_RATE_URL, String.class);
            if (responseStr == null) return;

            JsonNode root = objectMapper.readTree(responseStr);
            JsonNode rates = root.path("rates");
            double krwRate = rates.path("KRW").asDouble();

            if (krwRate <= 0) {
                log.warn("ExchangeRate-API returned invalid KRW rate: {}", krwRate);
                return;
            }

            String rateStr = String.valueOf(krwRate);
            redisTemplate.opsForValue().set(REDIS_KEY, rateStr);

            messagingTemplate.convertAndSend("/topic/exchange-rate", Map.of(
                    "rate", krwRate,
                    "updatedAt", java.time.LocalDateTime.now().toString()
            ));

            log.info("Exchange rate updated: 1 USD = {} KRW", krwRate);

        } catch (Exception e) {
            log.warn("Failed to fetch exchange rate: {}", e.getMessage());
        }
    }
}
