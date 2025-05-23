package com.learn.kafka.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.elasticsearch.client.ClientConfiguration;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchConfiguration;
import org.springframework.data.elasticsearch.client.elc.ReactiveElasticsearchConfiguration;
import org.springframework.data.elasticsearch.support.HttpHeaders;

import java.time.Duration;

@Configuration
public class ElasticsearchConfig extends ElasticsearchConfiguration {

    @Bean
    @Override
    public ClientConfiguration clientConfiguration() {
        HttpHeaders defaultHeaders = new HttpHeaders();
        defaultHeaders.add("X-Elastic-Product", "Elasticsearch");

        return ClientConfiguration.builder()
                .connectedTo("localhost:9200")
                .withConnectTimeout(Duration.ofSeconds(1))
                .withSocketTimeout(Duration.ofSeconds(30))
                .withDefaultHeaders(defaultHeaders)
                .build();
    }
} 