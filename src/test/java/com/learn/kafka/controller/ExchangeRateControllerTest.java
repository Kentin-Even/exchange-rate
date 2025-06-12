package com.learn.kafka.controller;

import com.learn.kafka.model.ExchangeRate;
import com.learn.kafka.service.ElasticsearchService;
import com.learn.kafka.service.ExchangeRateService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.request;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.asyncDispatch;

@WebMvcTest(ExchangeRateController.class)
@DisplayName("Tests unitaires pour ExchangeRateController")
class ExchangeRateControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ExchangeRateService exchangeRateService;

    @MockBean
    private ElasticsearchService elasticsearchService;

    @Autowired
    private ObjectMapper objectMapper;

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
    @DisplayName("GET /api/exchange-rates/fetch - Devrait retourner les taux de change")
    void fetchExchangeRates_Success() throws Exception {
        // Given
        when(exchangeRateService.fetchAndPublishExchangeRates())
            .thenReturn(Mono.just(sampleExchangeRate));

        // When & Then
        mockMvc.perform(get("/api/exchange-rates/fetch"))
            .andExpect(request().asyncStarted())
            .andDo(result -> {
                mockMvc.perform(asyncDispatch(result))
                    .andExpect(status().isOk())
                    .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                    .andExpect(jsonPath("$.id").value("test-id-123"))
                    .andExpect(jsonPath("$.base").value("USD"))
                    .andExpect(jsonPath("$.rates.EUR").value(0.85))
                    .andExpect(jsonPath("$.rates.GBP").value(0.75))
                    .andExpect(jsonPath("$.rates.JPY").value(110.0));
            });

        verify(exchangeRateService, times(1)).fetchAndPublishExchangeRates();
    }

    @Test
    @DisplayName("GET /api/exchange-rates/fetch - Devrait retourner 404 quand aucun résultat")
    void fetchExchangeRates_NotFound() throws Exception {
        // Given
        when(exchangeRateService.fetchAndPublishExchangeRates())
            .thenReturn(Mono.empty());

        // When & Then
        mockMvc.perform(get("/api/exchange-rates/fetch"))
            .andExpect(request().asyncStarted())
            .andDo(result -> {
                mockMvc.perform(asyncDispatch(result))
                    .andExpect(status().isNotFound());
            });

        verify(exchangeRateService, times(1)).fetchAndPublishExchangeRates();
    }

    @Test
    @DisplayName("POST /api/exchange-rates/test-elasticsearch - Devrait tester Elasticsearch")
    void testElasticsearch_Success() throws Exception {
        // Given
        when(elasticsearchService.saveExchangeRate(any(ExchangeRate.class)))
            .thenReturn(sampleExchangeRate);

        // When & Then
        mockMvc.perform(post("/api/exchange-rates/test-elasticsearch"))
            .andExpect(status().isOk())
            .andExpect(content().string("Test d'Elasticsearch réussi avec timestamp correct !"));

        verify(elasticsearchService, times(1)).saveExchangeRate(any(ExchangeRate.class));
    }

    @Test
    @DisplayName("POST /api/exchange-rates/test-elasticsearch - Devrait gérer les erreurs Elasticsearch")
    void testElasticsearch_Error() throws Exception {
        // Given
        when(elasticsearchService.saveExchangeRate(any(ExchangeRate.class)))
            .thenThrow(new RuntimeException("Erreur de connexion à Elasticsearch"));

        // When & Then
        mockMvc.perform(post("/api/exchange-rates/test-elasticsearch"))
            .andExpect(status().isBadRequest())
            .andExpect(content().string("Erreur Elasticsearch : Erreur de connexion à Elasticsearch"));

        verify(elasticsearchService, times(1)).saveExchangeRate(any(ExchangeRate.class));
    }

    @Test
    @DisplayName("GET /api/exchange-rates/fetch - Devrait gérer les erreurs du service")
    void fetchExchangeRates_ServiceError() throws Exception {
        // Given
        when(exchangeRateService.fetchAndPublishExchangeRates())
            .thenReturn(Mono.error(new RuntimeException("Erreur API externe")));

        // When & Then
        mockMvc.perform(get("/api/exchange-rates/fetch"))
            .andExpect(request().asyncStarted())
            .andDo(result -> {
                mockMvc.perform(asyncDispatch(result))
                    .andExpect(status().isInternalServerError());
            });

        verify(exchangeRateService, times(1)).fetchAndPublishExchangeRates();
    }
} 