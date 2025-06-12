package com.learn.kafka.controller;

import com.learn.kafka.model.ExchangeRate;
import com.learn.kafka.service.ElasticsearchService;
import com.learn.kafka.service.ExchangeRateService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/exchange-rates")
@RequiredArgsConstructor
public class ExchangeRateController {

    private final ExchangeRateService exchangeRateService;
    private final ElasticsearchService elasticsearchService;

    @GetMapping("/fetch")
    public Mono<ResponseEntity<ExchangeRate>> fetchExchangeRates() {
        return exchangeRateService.fetchAndPublishExchangeRates()
                .map(ResponseEntity::ok)
                .onErrorReturn(ResponseEntity.internalServerError().build())
                .defaultIfEmpty(ResponseEntity.notFound().build());
    }

    @PostMapping("/test-elasticsearch")
    public ResponseEntity<String> testElasticsearch() {
        try {
            ExchangeRate testRate = new ExchangeRate();
            testRate.setId("test-" + System.currentTimeMillis());
            testRate.setBaseCurrency("USD");
            testRate.setTimestamp(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            
            Map<String, Double> testRates = new HashMap<>();
            testRates.put("EUR", 0.85);
            testRates.put("GBP", 0.75);
            testRates.put("JPY", 110.0);
            testRate.setRates(testRates);
            
            elasticsearchService.saveExchangeRate(testRate);
            return ResponseEntity.ok("Test d'Elasticsearch réussi avec timestamp correct !");
        } catch (Exception e) {
            return ResponseEntity.badRequest()
                    .body("Erreur Elasticsearch : " + e.getMessage());
        }
    }
} 