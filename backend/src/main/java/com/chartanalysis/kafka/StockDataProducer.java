package com.chartanalysis.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class StockDataProducer {

    private static final String TOPIC = "stock-prices";
    private final KafkaTemplate<String, StockPriceMessage> kafkaTemplate;

    public void send(StockPriceMessage message) {
        kafkaTemplate.send(TOPIC, message.getSymbol(), message)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to send stock price for {}: {}", message.getSymbol(), ex.getMessage());
                    }
                });
    }
}
