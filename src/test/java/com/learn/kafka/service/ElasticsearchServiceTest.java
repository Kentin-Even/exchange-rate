package com.learn.kafka.service;

import com.learn.kafka.model.ExchangeRate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("Tests unitaires pour ElasticsearchService")
class ElasticsearchServiceTest {

    @Mock
    private ElasticsearchOperations elasticsearchOperations;

    @InjectMocks
    private ElasticsearchService elasticsearchService;

    private ExchangeRate sampleExchangeRate;

    @BeforeEach
    void setUp() {
        sampleExchangeRate = new ExchangeRate();
        sampleExchangeRate.setId("test-id-123");
        sampleExchangeRate.setBaseCurrency("USD");
        sampleExchangeRate.setTimestamp(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        
        Map<String, Double> rates = new HashMap<>();
        rates.put("EUR", 0.85);
        rates.put("GBP", 0.75);
        rates.put("JPY", 110.0);
        sampleExchangeRate.setRates(rates);
    }

    @Test
    @DisplayName("saveExchangeRate - Devrait sauvegarder un taux de change avec succès")
    void saveExchangeRate_Success() {
        // Given
        when(elasticsearchOperations.save(any(ExchangeRate.class)))
            .thenReturn(sampleExchangeRate);

        // When
        ExchangeRate result = elasticsearchService.saveExchangeRate(sampleExchangeRate);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo("test-id-123");
        assertThat(result.getBaseCurrency()).isEqualTo("USD");
        assertThat(result.getRates()).hasSize(3);
        assertThat(result.getRates().get("EUR")).isEqualTo(0.85);

        verify(elasticsearchOperations, times(1)).save(sampleExchangeRate);
    }

    @Test
    @DisplayName("saveExchangeRate - Devrait gérer les erreurs d'Elasticsearch")
    void saveExchangeRate_ElasticsearchError() {
        // Given
        when(elasticsearchOperations.save(any(ExchangeRate.class)))
            .thenThrow(new RuntimeException("Erreur de connexion Elasticsearch"));

        // When & Then
        assertThatThrownBy(() -> elasticsearchService.saveExchangeRate(sampleExchangeRate))
            .isInstanceOf(RuntimeException.class)
            .hasMessage("Erreur de connexion Elasticsearch");

        verify(elasticsearchOperations, times(1)).save(sampleExchangeRate);
    }

    @Test
    @DisplayName("saveExchangeRate - Devrait accepter un ExchangeRate null")
    void saveExchangeRate_NullInput() {
        // Given
        when(elasticsearchOperations.<ExchangeRate>save((ExchangeRate) null))
            .thenThrow(new IllegalArgumentException("ExchangeRate ne peut pas être null"));

        // When & Then
        assertThatThrownBy(() -> elasticsearchService.saveExchangeRate(null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessage("ExchangeRate ne peut pas être null");

        verify(elasticsearchOperations, times(1)).<ExchangeRate>save((ExchangeRate) null);
    }

    @Test
    @DisplayName("saveExchangeRate - Devrait sauvegarder un ExchangeRate avec des rates vides")
    void saveExchangeRate_EmptyRates() {
        // Given
        ExchangeRate emptyRatesExchangeRate = new ExchangeRate();
        emptyRatesExchangeRate.setId("empty-rates-id");
        emptyRatesExchangeRate.setBaseCurrency("USD");
        emptyRatesExchangeRate.setTimestamp(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        emptyRatesExchangeRate.setRates(new HashMap<>());

        when(elasticsearchOperations.save(any(ExchangeRate.class)))
            .thenReturn(emptyRatesExchangeRate);

        // When
        ExchangeRate result = elasticsearchService.saveExchangeRate(emptyRatesExchangeRate);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo("empty-rates-id");
        assertThat(result.getRates()).isEmpty();

        verify(elasticsearchOperations, times(1)).save(emptyRatesExchangeRate);
    }

    @Test
    @DisplayName("saveExchangeRate - Devrait sauvegarder un ExchangeRate avec beaucoup de devises")
    void saveExchangeRate_ManyRates() {
        // Given
        ExchangeRate manyRatesExchangeRate = new ExchangeRate();
        manyRatesExchangeRate.setId("many-rates-id");
        manyRatesExchangeRate.setBaseCurrency("USD");
        manyRatesExchangeRate.setTimestamp(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        
        Map<String, Double> manyRates = new HashMap<>();
        String[] currencies = {"EUR", "GBP", "JPY", "CHF", "CAD", "AUD", "SEK", "NOK", "DKK", "PLN"};
        for (int i = 0; i < currencies.length; i++) {
            manyRates.put(currencies[i], 1.0 + (i * 0.1));
        }
        manyRatesExchangeRate.setRates(manyRates);

        when(elasticsearchOperations.save(any(ExchangeRate.class)))
            .thenReturn(manyRatesExchangeRate);

        // When
        ExchangeRate result = elasticsearchService.saveExchangeRate(manyRatesExchangeRate);

        // Then
        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo("many-rates-id");
        assertThat(result.getRates()).hasSize(10);
        assertThat(result.getRates()).containsKey("EUR");
        assertThat(result.getRates()).containsKey("PLN");

        verify(elasticsearchOperations, times(1)).save(manyRatesExchangeRate);
    }

    @Test
    @DisplayName("Test de l'injection des dépendances")
    void testDependencyInjection() {
        // Vérifier que le service a bien été injecté avec le mock
        assertThat(elasticsearchService).isNotNull();
        
        // Test simple pour vérifier l'injection
        when(elasticsearchOperations.save(any(ExchangeRate.class)))
            .thenReturn(sampleExchangeRate);

        ExchangeRate result = elasticsearchService.saveExchangeRate(sampleExchangeRate);
        
        assertThat(result).isNotNull();
        verify(elasticsearchOperations, times(1)).save(any(ExchangeRate.class));
    }
} 