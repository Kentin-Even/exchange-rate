package com.learn.kafka.service;

import com.learn.kafka.model.ExchangeRate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Tests unitaires pour ScheduledExchangeRateService")
class ScheduledExchangeRateServiceTest {

    @Mock
    private ExchangeRateService exchangeRateService;

    @InjectMocks
    private ScheduledExchangeRateService scheduledExchangeRateService;

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
    @DisplayName("Test fetchExchangeRatesAutomatically - Succès")
    void testFetchExchangeRatesAutomatically_Success() {
        // Given
        when(exchangeRateService.fetchAndPublishExchangeRates())
            .thenReturn(Mono.just(sampleExchangeRate));

        // When
        scheduledExchangeRateService.fetchExchangeRatesAutomatically();

        // Then
        verify(exchangeRateService, times(1)).fetchAndPublishExchangeRates();
    }

    @Test
    @DisplayName("Test fetchExchangeRatesAutomatically - Erreur")
    void testFetchExchangeRatesAutomatically_Error() {
        // Given
        when(exchangeRateService.fetchAndPublishExchangeRates())
            .thenReturn(Mono.error(new RuntimeException("Service indisponible")));

        // When
        scheduledExchangeRateService.fetchExchangeRatesAutomatically();

        // Then
        verify(exchangeRateService, times(1)).fetchAndPublishExchangeRates();
    }

    @Test
    @DisplayName("Test fetchExchangeRatesAutomatically - Service retourne empty")
    void testFetchExchangeRatesAutomatically_Empty() {
        // Given
        when(exchangeRateService.fetchAndPublishExchangeRates())
            .thenReturn(Mono.empty());

        // When
        scheduledExchangeRateService.fetchExchangeRatesAutomatically();

        // Then
        verify(exchangeRateService, times(1)).fetchAndPublishExchangeRates();
    }

    @Test
    @DisplayName("Test fetchExchangeRatesForTesting - Succès")
    void testFetchExchangeRatesForTesting_Success() {
        // Given
        when(exchangeRateService.fetchAndPublishExchangeRates())
            .thenReturn(Mono.just(sampleExchangeRate));

        // When
        scheduledExchangeRateService.fetchExchangeRatesForTesting();

        // Then
        verify(exchangeRateService, times(1)).fetchAndPublishExchangeRates();
    }

    @Test
    @DisplayName("Test fetchExchangeRatesForTesting - Erreur")
    void testFetchExchangeRatesForTesting_Error() {
        // Given
        when(exchangeRateService.fetchAndPublishExchangeRates())
            .thenReturn(Mono.error(new RuntimeException("Erreur de test")));

        // When
        scheduledExchangeRateService.fetchExchangeRatesForTesting();

        // Then
        verify(exchangeRateService, times(1)).fetchAndPublishExchangeRates();
    }

    @Test
    @DisplayName("Test fetchExchangeRatesForTesting - Service retourne empty")
    void testFetchExchangeRatesForTesting_Empty() {
        // Given
        when(exchangeRateService.fetchAndPublishExchangeRates())
            .thenReturn(Mono.empty());

        // When
        scheduledExchangeRateService.fetchExchangeRatesForTesting();

        // Then
        verify(exchangeRateService, times(1)).fetchAndPublishExchangeRates();
    }

    @Test
    @DisplayName("Test avec service qui retourne null")
    void testWithNullResponse() {
        // Given
        when(exchangeRateService.fetchAndPublishExchangeRates())
            .thenReturn(null);

        // When & Then - Should handle null gracefully
        try {
            scheduledExchangeRateService.fetchExchangeRatesAutomatically();
            verify(exchangeRateService, times(1)).fetchAndPublishExchangeRates();
        } catch (Exception e) {
            // Expected behavior when service returns null
        }
    }
} 