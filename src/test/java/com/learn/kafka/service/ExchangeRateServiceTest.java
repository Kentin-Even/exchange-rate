package com.learn.kafka.service;

import com.learn.kafka.model.ExchangeRate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Tests unitaires pour ExchangeRateService")
class ExchangeRateServiceTest {

    @Mock
    private WebClient webClient;

    @Mock
    private WebClient.RequestHeadersUriSpec requestHeadersUriSpec;

    @Mock
    private WebClient.RequestHeadersSpec requestHeadersSpec;

    @Mock
    private WebClient.ResponseSpec responseSpec;

    @Mock
    private KafkaTemplate<String, ExchangeRate> exchangeRateKafkaTemplate;

    @InjectMocks
    private ExchangeRateService exchangeRateService;

    private ExchangeRate sampleExchangeRate;

    @BeforeEach
    void setUp() {
        sampleExchangeRate = new ExchangeRate();
        sampleExchangeRate.setBaseCurrency("USD");
        
        Map<String, Double> rates = new HashMap<>();
        rates.put("EUR", 0.85);
        rates.put("GBP", 0.75);
        rates.put("JPY", 110.0);
        sampleExchangeRate.setRates(rates);
    }

    @Test
    @DisplayName("fetchAndPublishExchangeRates - Devrait récupérer et publier les taux de change avec succès")
    void fetchAndPublishExchangeRates_Success() {
        // Given
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri("/v4/latest/USD")).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(ExchangeRate.class)).thenReturn(Mono.just(sampleExchangeRate));

        // When & Then
        StepVerifier.create(exchangeRateService.fetchAndPublishExchangeRates())
            .assertNext(result -> {
                assertThat(result).isNotNull();
                assertThat(result.getBaseCurrency()).isEqualTo("USD");
                assertThat(result.getRates()).hasSize(3);
                assertThat(result.getRates().get("EUR")).isEqualTo(0.85);
                assertThat(result.getId()).isNotNull(); // UUID généré
                assertThat(result.getTimestamp()).isNotNull(); // Timestamp généré
                
                // Vérifier le format du timestamp
                assertThat(result.getTimestamp()).matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}.*");
            })
            .verifyComplete();

        // Vérifier que le message a été envoyé vers Kafka
        verify(exchangeRateKafkaTemplate, times(1))
            .send(eq("exchange-rates"), any(ExchangeRate.class));
    }

    @Test
    @DisplayName("fetchAndPublishExchangeRates - Devrait gérer les erreurs de l'API externe")
    void fetchAndPublishExchangeRates_ApiError() {
        // Given
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri("/v4/latest/USD")).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(ExchangeRate.class))
            .thenReturn(Mono.error(new WebClientResponseException(500, "Internal Server Error", null, null, null)));

        // When & Then
        StepVerifier.create(exchangeRateService.fetchAndPublishExchangeRates())
            .expectError(WebClientResponseException.class)
            .verify();

        // Vérifier qu'aucun message n'a été envoyé vers Kafka en cas d'erreur
        verify(exchangeRateKafkaTemplate, never())
            .send(anyString(), any(ExchangeRate.class));
    }

    @Test
    @DisplayName("fetchAndPublishExchangeRates - Devrait gérer une réponse vide de l'API")
    void fetchAndPublishExchangeRates_EmptyResponse() {
        // Given
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri("/v4/latest/USD")).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(ExchangeRate.class)).thenReturn(Mono.empty());

        // When & Then
        StepVerifier.create(exchangeRateService.fetchAndPublishExchangeRates())
            .verifyComplete();

        // Vérifier qu'aucun message n'a été envoyé vers Kafka
        verify(exchangeRateKafkaTemplate, never())
            .send(anyString(), any(ExchangeRate.class));
    }

    @Test
    @DisplayName("fetchAndPublishExchangeRates - Devrait gérer une erreur de timeout")
    void fetchAndPublishExchangeRates_Timeout() {
        // Given
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri("/v4/latest/USD")).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(ExchangeRate.class))
            .thenReturn(Mono.error(new RuntimeException("Timeout")));

        // When & Then
        StepVerifier.create(exchangeRateService.fetchAndPublishExchangeRates())
            .expectError(RuntimeException.class)
            .verify();

        verify(exchangeRateKafkaTemplate, never())
            .send(anyString(), any(ExchangeRate.class));
    }

    @Test
    @DisplayName("fetchAndPublishExchangeRates - Devrait générer un ID unique à chaque appel")
    void fetchAndPublishExchangeRates_UniqueIds() {
        // Given
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri("/v4/latest/USD")).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(ExchangeRate.class)).thenReturn(Mono.just(sampleExchangeRate));

        // When & Then - Effectuer deux appels séparés et vérifier que les IDs sont différents
        String firstId = exchangeRateService.fetchAndPublishExchangeRates().block().getId();
        String secondId = exchangeRateService.fetchAndPublishExchangeRates().block().getId();
        
        assertThat(firstId).isNotNull();
        assertThat(secondId).isNotNull();
        assertThat(firstId).isNotEqualTo(secondId);

        verify(exchangeRateKafkaTemplate, times(2))
            .send(eq("exchange-rates"), any(ExchangeRate.class));
    }

    @Test
    @DisplayName("fetchAndPublishExchangeRates - Devrait gérer un ExchangeRate avec rates null")
    void fetchAndPublishExchangeRates_NullRates() {
        // Given
        ExchangeRate exchangeRateWithNullRates = new ExchangeRate();
        exchangeRateWithNullRates.setBaseCurrency("USD");
        exchangeRateWithNullRates.setRates(null);

        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri("/v4/latest/USD")).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(ExchangeRate.class)).thenReturn(Mono.just(exchangeRateWithNullRates));

        // When & Then
        StepVerifier.create(exchangeRateService.fetchAndPublishExchangeRates())
            .assertNext(result -> {
                assertThat(result).isNotNull();
                assertThat(result.getBaseCurrency()).isEqualTo("USD");
                assertThat(result.getRates()).isNull();
                assertThat(result.getId()).isNotNull();
                assertThat(result.getTimestamp()).isNotNull();
            })
            .verifyComplete();

        verify(exchangeRateKafkaTemplate, times(1))
            .send(eq("exchange-rates"), any(ExchangeRate.class));
    }

    @Test
    @DisplayName("fetchAndPublishExchangeRates - Devrait gérer un ExchangeRate avec rates vides")
    void fetchAndPublishExchangeRates_EmptyRates() {
        // Given
        ExchangeRate exchangeRateWithEmptyRates = new ExchangeRate();
        exchangeRateWithEmptyRates.setBaseCurrency("USD");
        exchangeRateWithEmptyRates.setRates(new HashMap<>());

        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri("/v4/latest/USD")).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(ExchangeRate.class)).thenReturn(Mono.just(exchangeRateWithEmptyRates));

        // When & Then
        StepVerifier.create(exchangeRateService.fetchAndPublishExchangeRates())
            .assertNext(result -> {
                assertThat(result).isNotNull();
                assertThat(result.getBaseCurrency()).isEqualTo("USD");
                assertThat(result.getRates()).isEmpty();
                assertThat(result.getId()).isNotNull();
                assertThat(result.getTimestamp()).isNotNull();
            })
            .verifyComplete();

        verify(exchangeRateKafkaTemplate, times(1))
            .send(eq("exchange-rates"), any(ExchangeRate.class));
    }

    @Test
    @DisplayName("fetchAndPublishExchangeRates - Devrait gérer les erreurs Kafka sans propager l'exception")
    void fetchAndPublishExchangeRates_KafkaError() {
        // Given
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri("/v4/latest/USD")).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(ExchangeRate.class)).thenReturn(Mono.just(sampleExchangeRate));
        
        doThrow(new RuntimeException("Kafka connection failed"))
            .when(exchangeRateKafkaTemplate).send(anyString(), any(ExchangeRate.class));

        // When & Then - Le service devrait gérer l'erreur Kafka gracieusement
        StepVerifier.create(exchangeRateService.fetchAndPublishExchangeRates())
            .expectError(RuntimeException.class)
            .verify();

        verify(exchangeRateKafkaTemplate, times(1))
            .send(eq("exchange-rates"), any(ExchangeRate.class));
    }

    @Test
    @DisplayName("Test de l'injection des dépendances")
    void testDependencyInjection() {
        // Vérifier que le service a bien été injecté avec les mocks
        assertThat(exchangeRateService).isNotNull();
    }

    @Test
    @DisplayName("fetchAndPublishExchangeRates - Devrait utiliser le bon path d'API")
    void fetchAndPublishExchangeRates_CorrectApiPath() {
        // Given
        when(webClient.get()).thenReturn(requestHeadersUriSpec);
        when(requestHeadersUriSpec.uri("/v4/latest/USD")).thenReturn(requestHeadersSpec);
        when(requestHeadersSpec.retrieve()).thenReturn(responseSpec);
        when(responseSpec.bodyToMono(ExchangeRate.class)).thenReturn(Mono.just(sampleExchangeRate));

        // When
        StepVerifier.create(exchangeRateService.fetchAndPublishExchangeRates())
            .assertNext(result -> assertThat(result).isNotNull())
            .verifyComplete();

        // Then - Vérifier que le bon path a été utilisé
        verify(requestHeadersUriSpec, times(1)).uri("/v4/latest/USD");
    }
} 