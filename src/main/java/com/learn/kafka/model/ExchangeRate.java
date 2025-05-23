package com.learn.kafka.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Document(indexName = "exchange_rates")
public class ExchangeRate {
    @Id
    private String id;
    
    @Field(type = FieldType.Keyword)
    @JsonProperty("base")
    private String baseCurrency;
    
    @Field(type = FieldType.Object)
    private Map<String, Double> rates;
    
    @Field(type = FieldType.Text)
    private String timestamp;
} 