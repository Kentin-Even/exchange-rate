package com.learn.kafka.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Tests unitaires pour le modèle ExchangeRate")
class ExchangeRateTest {

    private ExchangeRate exchangeRate;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        exchangeRate = new ExchangeRate();
    }

    @Test
    @DisplayName("Test des getters et setters")
    void testGettersAndSetters() {
        // Given
        String id = "test-id-123";
        String baseCurrency = "USD";
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        
        Map<String, Double> rates = new HashMap<>();
        rates.put("EUR", 0.85);
        rates.put("GBP", 0.75);
        rates.put("JPY", 110.0);

        // When
        exchangeRate.setId(id);
        exchangeRate.setBaseCurrency(baseCurrency);
        exchangeRate.setTimestamp(timestamp);
        exchangeRate.setRates(rates);

        // Then
        assertThat(exchangeRate.getId()).isEqualTo(id);
        assertThat(exchangeRate.getBaseCurrency()).isEqualTo(baseCurrency);
        assertThat(exchangeRate.getTimestamp()).isEqualTo(timestamp);
        assertThat(exchangeRate.getRates()).isEqualTo(rates);
        assertThat(exchangeRate.getRates()).hasSize(3);
    }

    @Test
    @DisplayName("Test de sérialisation JSON")
    void testJsonSerialization() throws JsonProcessingException {
        // Given
        exchangeRate.setId("test-123");
        exchangeRate.setBaseCurrency("USD");
        exchangeRate.setTimestamp("2025-06-04T12:00:00");
        
        Map<String, Double> rates = new HashMap<>();
        rates.put("EUR", 0.85);
        rates.put("GBP", 0.75);
        exchangeRate.setRates(rates);

        // When
        String json = objectMapper.writeValueAsString(exchangeRate);

        // Then
        assertThat(json).contains("\"id\":\"test-123\"");
        assertThat(json).contains("\"base\":\"USD\"");
        assertThat(json).contains("\"timestamp\":\"2025-06-04T12:00:00\"");
        assertThat(json).contains("\"EUR\":0.85");
        assertThat(json).contains("\"GBP\":0.75");
    }

    @Test
    @DisplayName("Test de désérialisation JSON")
    void testJsonDeserialization() throws JsonProcessingException {
        // Given
        String json = "{\"id\":\"test-456\",\"base\":\"EUR\",\"timestamp\":\"2025-06-04T15:30:00\",\"rates\":{\"USD\":1.18,\"GBP\":0.88}}";

        // When
        ExchangeRate deserializedRate = objectMapper.readValue(json, ExchangeRate.class);

        // Then
        assertThat(deserializedRate.getId()).isEqualTo("test-456");
        assertThat(deserializedRate.getBaseCurrency()).isEqualTo("EUR");
        assertThat(deserializedRate.getTimestamp()).isEqualTo("2025-06-04T15:30:00");
        assertThat(deserializedRate.getRates()).hasSize(2);
        assertThat(deserializedRate.getRates().get("USD")).isEqualTo(1.18);
        assertThat(deserializedRate.getRates().get("GBP")).isEqualTo(0.88);
    }

    @Test
    @DisplayName("Test equals et hashCode")
    void testEqualsAndHashCode() {
        // Given
        ExchangeRate rate1 = new ExchangeRate();
        rate1.setId("same-id");
        rate1.setBaseCurrency("USD");
        
        ExchangeRate rate2 = new ExchangeRate();
        rate2.setId("same-id");
        rate2.setBaseCurrency("USD");
        
        ExchangeRate rate3 = new ExchangeRate();
        rate3.setId("different-id");
        rate3.setBaseCurrency("EUR");

        // Then
        assertThat(rate1).isEqualTo(rate2);
        assertThat(rate1).isNotEqualTo(rate3);
        assertThat(rate1.hashCode()).isEqualTo(rate2.hashCode());
    }

    @Test
    @DisplayName("Test toString")
    void testToString() {
        // Given
        exchangeRate.setId("test-789");
        exchangeRate.setBaseCurrency("JPY");
        
        Map<String, Double> rates = new HashMap<>();
        rates.put("USD", 0.009);
        exchangeRate.setRates(rates);

        // When
        String toString = exchangeRate.toString();

        // Then
        assertThat(toString).contains("test-789");
        assertThat(toString).contains("JPY");
        assertThat(toString).contains("USD");
    }

    @Test
    @DisplayName("Test avec des valeurs nulles")
    void testWithNullValues() {
        // When
        exchangeRate.setId(null);
        exchangeRate.setBaseCurrency(null);
        exchangeRate.setTimestamp(null);
        exchangeRate.setRates(null);

        // Then - Should not throw exceptions
        assertThat(exchangeRate.getId()).isNull();
        assertThat(exchangeRate.getBaseCurrency()).isNull();
        assertThat(exchangeRate.getTimestamp()).isNull();
        assertThat(exchangeRate.getRates()).isNull();
    }

    @Test
    @DisplayName("Test avec des rates vides")
    void testWithEmptyRates() {
        // Given
        Map<String, Double> emptyRates = new HashMap<>();
        
        // When
        exchangeRate.setRates(emptyRates);

        // Then
        assertThat(exchangeRate.getRates()).isNotNull();
        assertThat(exchangeRate.getRates()).isEmpty();
    }
} 