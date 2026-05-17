package com.chartanalysis.kafka;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
@RequiredArgsConstructor
public class FinnhubWebSocketClient {

    private final StockDataProducer producer;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${finnhub.api-key}")
    private String apiKey;

    @Value("${finnhub.us-symbols}")
    private String usSymbolsRaw;

    private final OkHttpClient httpClient = new OkHttpClient();
    private final Map<String, BigDecimal> latestPrices = new ConcurrentHashMap<>();
    private volatile WebSocket webSocket;

    @PostConstruct
    public void connect() {
        List<String> symbols = List.of(usSymbolsRaw.split(","));
        connectWebSocket(symbols);
    }

    private void connectWebSocket(List<String> symbols) {
        String url = "wss://ws.finnhub.io?token=" + apiKey;
        Request request = new Request.Builder().url(url).build();

        webSocket = httpClient.newWebSocket(request, new WebSocketListener() {
            @Override
            public void onOpen(@NotNull WebSocket ws, @NotNull Response response) {
                log.info("Finnhub WebSocket connected");
                for (String symbol : symbols) {
                    String subscribeMsg = "{\"type\":\"subscribe\",\"symbol\":\"" + symbol.trim() + "\"}";
                    ws.send(subscribeMsg);
                    log.debug("Subscribed to Finnhub symbol: {}", symbol.trim());
                }
            }

            @Override
            public void onMessage(@NotNull WebSocket ws, @NotNull String text) {
                try {
                    JsonNode root = objectMapper.readTree(text);
                    String type = root.path("type").asText();

                    if ("ping".equals(type)) {
                        // Heartbeat from Finnhub — ignore
                        return;
                    }

                    if ("trade".equals(type)) {
                        JsonNode dataArray = root.path("data");
                        if (dataArray.isArray()) {
                            for (JsonNode trade : dataArray) {
                                String symbol = trade.path("s").asText();
                                double price = trade.path("p").asDouble();
                                if (!symbol.isEmpty() && price > 0) {
                                    latestPrices.put(symbol, BigDecimal.valueOf(price));
                                }
                            }
                        }
                    }
                } catch (Exception e) {
                    log.warn("Error parsing Finnhub message: {}", e.getMessage());
                }
            }

            @Override
            public void onFailure(@NotNull WebSocket ws, @NotNull Throwable t, Response response) {
                log.error("Finnhub WebSocket failure: {}", t.getMessage());
                scheduleReconnect(symbols);
            }

            @Override
            public void onClosed(@NotNull WebSocket ws, int code, @NotNull String reason) {
                log.warn("Finnhub WebSocket closed: {} {}", code, reason);
                if (code != 1000) {
                    scheduleReconnect(symbols);
                }
            }
        });
    }

    private void scheduleReconnect(List<String> symbols) {
        Thread reconnectThread = new Thread(() -> {
            try {
                Thread.sleep(5000);
                log.info("Reconnecting to Finnhub WebSocket...");
                connectWebSocket(symbols);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        });
        reconnectThread.setDaemon(true);
        reconnectThread.start();
    }

    @Scheduled(fixedDelay = 3000)
    public void publishLatestPrices() {
        if (latestPrices.isEmpty()) return;

        latestPrices.forEach((symbol, price) -> {
            StockPriceMessage msg = new StockPriceMessage(
                    symbol,
                    price.setScale(2, java.math.RoundingMode.HALF_UP),
                    BigDecimal.ZERO,
                    0L,
                    LocalDateTime.now()
            );
            // Use price for all OHLC fields when only trade price is available
            msg.setOpenPrice(price);
            msg.setHighPrice(price);
            msg.setLowPrice(price);
            producer.send(msg);
            log.debug("Finnhub: {} -> {}", symbol, price);
        });
    }
}
