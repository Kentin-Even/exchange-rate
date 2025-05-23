package com.learn.kafka.service;

import com.learn.kafka.model.ExchangeRate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ElasticsearchService {
    private final ElasticsearchOperations elasticsearchOperations;

    public ExchangeRate saveExchangeRate(ExchangeRate exchangeRate) {
        return elasticsearchOperations.save(exchangeRate);
    }
} 