package com.learn.kafka.consumer;

import com.learn.kafka.model.ExchangeRate;
import com.learn.kafka.service.ElasticsearchService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Tests unitaires pour ExchangeRateConsumer")
class ExchangeRateConsumerTest {

    @Mock
    private ElasticsearchService elasticsearchService;

    @InjectMocks
    private ExchangeRateConsumer exchangeRateConsumer;

    private ExchangeRate sampleExchangeRate;

    @BeforeEach
    void setUp() {
        sampleExchangeRate = createSampleExchangeRate();
    }

    private ExchangeRate createSampleExchangeRate() {
        ExchangeRate rate = new ExchangeRate();
        rate.setId("test-id-123");
        rate.setBaseCurrency("USD");
        rate.setTimestamp(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        
        Map<String, Double> rates = new HashMap<>();
        rates.put("EUR", 0.85);
        rates.put("GBP", 0.75);
        rates.put("JPY", 110.0);
        rate.setRates(rates);
        
        return rate;
    }

    @Test
    @DisplayName("Test consumeExchangeRate - Succès")
    void testConsumeExchangeRate_Success() {
        // Given
        when(elasticsearchService.saveExchangeRate(sampleExchangeRate))
            .thenReturn(sampleExchangeRate);

        // When
        exchangeRateConsumer.consumeExchangeRate(sampleExchangeRate);

        // Then
        verify(elasticsearchService, times(1)).saveExchangeRate(sampleExchangeRate);
    }

    @Test
    @DisplayName("Test consumeExchangeRate - Erreur Elasticsearch")
    void testConsumeExchangeRate_ElasticsearchError() {
        // Given
        when(elasticsearchService.saveExchangeRate(sampleExchangeRate))
            .thenThrow(new RuntimeException("Elasticsearch connection failed"));

        // When
        exchangeRateConsumer.consumeExchangeRate(sampleExchangeRate);

        // Then
        verify(elasticsearchService, times(1)).saveExchangeRate(sampleExchangeRate);
    }

    @Test
    @DisplayName("Test consumeExchangeRate - Avec différentes devises")
    void testConsumeExchangeRate_DifferentCurrencies() {
        // Given
        ExchangeRate eurRate = createSampleExchangeRate();
        eurRate.setBaseCurrency("EUR");
        eurRate.setId("eur-id-456");
        
        when(elasticsearchService.saveExchangeRate(eurRate))
            .thenReturn(eurRate);

        // When
        exchangeRateConsumer.consumeExchangeRate(eurRate);

        // Then
        verify(elasticsearchService, times(1)).saveExchangeRate(eurRate);
    }

    @Test
    @DisplayName("Test consumeExchangeRate - Avec rates vides")
    void testConsumeExchangeRate_EmptyRates() {
        // Given
        ExchangeRate emptyRate = new ExchangeRate();
        emptyRate.setId("empty-id-789");
        emptyRate.setBaseCurrency("GBP");
        emptyRate.setTimestamp(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        emptyRate.setRates(new HashMap<>());
        
        when(elasticsearchService.saveExchangeRate(emptyRate))
            .thenReturn(emptyRate);

        // When
        exchangeRateConsumer.consumeExchangeRate(emptyRate);

        // Then
        verify(elasticsearchService, times(1)).saveExchangeRate(emptyRate);
    }

    @Test
    @DisplayName("Test consumeExchangeRate - Avec timestamp null")
    void testConsumeExchangeRate_NullTimestamp() {
        // Given
        ExchangeRate nullTimestampRate = createSampleExchangeRate();
        nullTimestampRate.setTimestamp(null);
        
        when(elasticsearchService.saveExchangeRate(nullTimestampRate))
            .thenReturn(nullTimestampRate);

        // When
        exchangeRateConsumer.consumeExchangeRate(nullTimestampRate);

        // Then
        verify(elasticsearchService, times(1)).saveExchangeRate(nullTimestampRate);
    }

    @Test
    @DisplayName("Test consumeExchangeRate - Exception récupérée")
    void testConsumeExchangeRate_RuntimeException() {
        // Given
        doThrow(new RuntimeException("Unexpected error")).when(elasticsearchService).saveExchangeRate(any());

        // When & Then - L'exception doit être gérée et loggée, pas propagée
        exchangeRateConsumer.consumeExchangeRate(sampleExchangeRate);
        
        verify(elasticsearchService, times(1)).saveExchangeRate(sampleExchangeRate);
    }
} 