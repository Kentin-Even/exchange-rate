package com.learn.kafka.consumer;

import com.learn.kafka.model.ExchangeRate;
import com.learn.kafka.service.ElasticsearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor
public class ExchangeRateConsumer {

    private final ElasticsearchService elasticsearchService;

    @KafkaListener(
        topics = "exchange-rates", 
        groupId = "exchange-rate-group",
        containerFactory = "exchangeRateKafkaListenerContainerFactory"
    )
    public void consumeExchangeRate(ExchangeRate exchangeRate) {
        log.info("Received exchange rate: {} with timestamp: {}", 
                 exchangeRate.getBaseCurrency(), exchangeRate.getTimestamp());
        
        try {
            elasticsearchService.saveExchangeRate(exchangeRate);
            log.info("Exchange rate saved to Elasticsearch successfully");
        } catch (Exception e) {
            log.error("Failed to save exchange rate to Elasticsearch: {}", e.getMessage());
        }
    }
} 