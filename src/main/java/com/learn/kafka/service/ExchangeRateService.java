package com.learn.kafka.service;

import com.learn.kafka.model.ExchangeRate;
import lombok.RequiredArgsConstructor;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ExchangeRateService {
    private final WebClient webClient;
    private final KafkaTemplate<String, ExchangeRate> exchangeRateKafkaTemplate;
    private static final String EXCHANGE_RATE_TOPIC = "exchange-rates";
    private static final String API_PATH = "/v4/latest/USD";

    public Mono<ExchangeRate> fetchAndPublishExchangeRates() {
        return webClient.get()
                .uri(API_PATH)
                .retrieve()
                .bodyToMono(ExchangeRate.class)
                .map(rate -> {
                    rate.setId(UUID.randomUUID().toString());
                    rate.setTimestamp(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
                    exchangeRateKafkaTemplate.send(EXCHANGE_RATE_TOPIC, rate);
                    return rate;
                });
    }
}