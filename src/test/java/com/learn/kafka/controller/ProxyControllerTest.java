package com.learn.kafka.controller;

import com.learn.kafka.model.ExchangeRate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.TotalHitsRelation;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProxyController.class)
@DisplayName("Tests unitaires pour ProxyController")
class ProxyControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ElasticsearchOperations elasticsearchOperations;

    private ExchangeRate sampleExchangeRate;
    private SearchHits<ExchangeRate> mockSearchHits;
    private SearchHit<ExchangeRate> mockSearchHit;

    @BeforeEach
    void setUp() {
        // Créer des données de test
        sampleExchangeRate = new ExchangeRate();
        sampleExchangeRate.setId("test-id-123");
        sampleExchangeRate.setBaseCurrency("USD");
        sampleExchangeRate.setTimestamp(LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
        
        Map<String, Double> rates = new HashMap<>();
        rates.put("EUR", 0.85);
        rates.put("GBP", 0.75);
        rates.put("JPY", 110.0);
        sampleExchangeRate.setRates(rates);

        // Mock des SearchHits
        mockSearchHit = mock(SearchHit.class);
        when(mockSearchHit.getContent()).thenReturn(sampleExchangeRate);

        mockSearchHits = mock(SearchHits.class);
        when(mockSearchHits.hasSearchHits()).thenReturn(true);
        when(mockSearchHits.getSearchHit(0)).thenReturn(mockSearchHit);
        when(mockSearchHits.getTotalHits()).thenReturn(1L);
        when(mockSearchHits.getTotalHitsRelation()).thenReturn(TotalHitsRelation.EQUAL_TO);
        when(mockSearchHits.stream()).thenReturn(Arrays.asList(mockSearchHit).stream());
    }

    @Test
    @DisplayName("GET /api/proxy/test - Devrait retourner un message de confirmation")
    void getTest_Success() throws Exception {
        // When & Then
        mockMvc.perform(get("/api/proxy/test"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.TEXT_PLAIN_VALUE + ";charset=UTF-8"))
            .andExpect(content().string("Proxy controller is working!"));

        // Vérifier qu'aucune interaction avec Elasticsearch n'a eu lieu
        verifyNoInteractions(elasticsearchOperations);
    }

    @Test
    @DisplayName("GET /api/proxy/simple - Devrait retourner tous les taux sans filtre")
    void getSimpleData_Success() throws Exception {
        // Given
        when(elasticsearchOperations.search(any(Query.class), eq(ExchangeRate.class)))
            .thenReturn(mockSearchHits);

        // When & Then
        mockMvc.perform(get("/api/proxy/simple"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$[0].id").value("test-id-123"))
            .andExpect(jsonPath("$[0].base").value("USD"))
            .andExpect(jsonPath("$[0].rates.EUR").value(0.85))
            .andExpect(jsonPath("$[0].rates.GBP").value(0.75))
            .andExpect(jsonPath("$[0].rates.JPY").value(110.0));

        verify(elasticsearchOperations, times(1)).search(any(Query.class), eq(ExchangeRate.class));
    }

    @Test
    @DisplayName("GET /api/proxy/simple - Devrait gérer les erreurs Elasticsearch")
    void getSimpleData_Error() throws Exception {
        // Given
        when(elasticsearchOperations.search(any(Query.class), eq(ExchangeRate.class)))
            .thenThrow(new RuntimeException("Erreur Elasticsearch"));

        // When & Then
        mockMvc.perform(get("/api/proxy/simple"))
            .andExpect(status().isInternalServerError());

        verify(elasticsearchOperations, times(1)).search(any(Query.class), eq(ExchangeRate.class));
    }

    @Test
    @DisplayName("GET /api/proxy/simple - Devrait retourner une liste vide quand aucun résultat")
    void getSimpleData_EmptyResults() throws Exception {
        // Given
        SearchHits<ExchangeRate> emptySearchHits = mock(SearchHits.class);
        when(emptySearchHits.hasSearchHits()).thenReturn(false);
        when(emptySearchHits.getTotalHits()).thenReturn(0L);
        when(emptySearchHits.stream()).thenReturn(Arrays.<SearchHit<ExchangeRate>>asList().stream());

        when(elasticsearchOperations.search(any(Query.class), eq(ExchangeRate.class)))
            .thenReturn(emptySearchHits);

        // When & Then
        mockMvc.perform(get("/api/proxy/simple"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$").isEmpty());

        verify(elasticsearchOperations, times(1)).search(any(Query.class), eq(ExchangeRate.class));
    }

    @Test
    @DisplayName("GET /api/proxy/latest-rates - Devrait retourner le dernier taux de change")
    void getLatestExchangeRates_Success() throws Exception {
        // Given
        when(elasticsearchOperations.search(any(Query.class), eq(ExchangeRate.class)))
            .thenReturn(mockSearchHits);

        // When & Then
        mockMvc.perform(get("/api/proxy/latest-rates"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$.id").value("test-id-123"))
            .andExpect(jsonPath("$.base").value("USD"))
            .andExpect(jsonPath("$.rates.EUR").value(0.85));

        verify(elasticsearchOperations, times(1)).search(any(Query.class), eq(ExchangeRate.class));
    }

    @Test
    @DisplayName("GET /api/proxy/latest-rates - Devrait retourner 404 quand aucun résultat")
    void getLatestExchangeRates_NotFound() throws Exception {
        // Given
        SearchHits<ExchangeRate> emptySearchHits = mock(SearchHits.class);
        when(emptySearchHits.hasSearchHits()).thenReturn(false);
        when(emptySearchHits.getTotalHits()).thenReturn(0L);

        when(elasticsearchOperations.search(any(Query.class), eq(ExchangeRate.class)))
            .thenReturn(emptySearchHits);

        // When & Then
        mockMvc.perform(get("/api/proxy/latest-rates"))
            .andExpect(status().isNotFound());

        verify(elasticsearchOperations, times(1)).search(any(Query.class), eq(ExchangeRate.class));
    }

    @Test
    @DisplayName("GET /api/proxy/latest-rates - Devrait gérer les erreurs Elasticsearch")
    void getLatestExchangeRates_Error() throws Exception {
        // Given
        when(elasticsearchOperations.search(any(Query.class), eq(ExchangeRate.class)))
            .thenThrow(new RuntimeException("Connexion Elasticsearch échouée"));

        // When & Then
        mockMvc.perform(get("/api/proxy/latest-rates"))
            .andExpect(status().isInternalServerError());

        verify(elasticsearchOperations, times(1)).search(any(Query.class), eq(ExchangeRate.class));
    }

    @Test
    @DisplayName("GET /api/proxy/all-rates - Devrait retourner tous les taux de change")
    void getAllExchangeRates_Success() throws Exception {
        // Given
        when(elasticsearchOperations.search(any(Query.class), eq(ExchangeRate.class)))
            .thenReturn(mockSearchHits);

        // When & Then
        mockMvc.perform(get("/api/proxy/all-rates"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(jsonPath("$").isArray())
            .andExpect(jsonPath("$[0].id").value("test-id-123"))
            .andExpect(jsonPath("$[0].base").value("USD"));

        verify(elasticsearchOperations, times(1)).search(any(Query.class), eq(ExchangeRate.class));
    }

    @Test
    @DisplayName("GET /api/proxy/all-rates - Devrait gérer les erreurs Elasticsearch")
    void getAllExchangeRates_Error() throws Exception {
        // Given
        when(elasticsearchOperations.search(any(Query.class), eq(ExchangeRate.class)))
            .thenThrow(new RuntimeException("Erreur de requête Elasticsearch"));

        // When & Then
        mockMvc.perform(get("/api/proxy/all-rates"))
            .andExpect(status().isInternalServerError());

        verify(elasticsearchOperations, times(1)).search(any(Query.class), eq(ExchangeRate.class));
    }

    @Test
    @DisplayName("GET /api/proxy/rate/{currency} - Devrait retourner le taux pour une devise spécifique")
    void getSpecificRate_Success() throws Exception {
        // Given
        when(elasticsearchOperations.search(any(Query.class), eq(ExchangeRate.class)))
            .thenReturn(mockSearchHits);

        // When & Then
        mockMvc.perform(get("/api/proxy/rate/EUR"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(content().string("0.85"));

        verify(elasticsearchOperations, times(1)).search(any(Query.class), eq(ExchangeRate.class));
    }

    @Test
    @DisplayName("GET /api/proxy/rate/{currency} - Devrait retourner le taux pour une devise en minuscules")
    void getSpecificRate_LowerCase() throws Exception {
        // Given
        when(elasticsearchOperations.search(any(Query.class), eq(ExchangeRate.class)))
            .thenReturn(mockSearchHits);

        // When & Then
        mockMvc.perform(get("/api/proxy/rate/eur"))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.APPLICATION_JSON))
            .andExpect(content().string("0.85"));

        verify(elasticsearchOperations, times(1)).search(any(Query.class), eq(ExchangeRate.class));
    }

    @Test
    @DisplayName("GET /api/proxy/rate/{currency} - Devrait retourner 404 pour une devise inexistante")
    void getSpecificRate_CurrencyNotFound() throws Exception {
        // Given
        when(elasticsearchOperations.search(any(Query.class), eq(ExchangeRate.class)))
            .thenReturn(mockSearchHits);

        // When & Then
        mockMvc.perform(get("/api/proxy/rate/XYZ"))
            .andExpect(status().isNotFound());

        verify(elasticsearchOperations, times(1)).search(any(Query.class), eq(ExchangeRate.class));
    }

    @Test
    @DisplayName("GET /api/proxy/rate/{currency} - Devrait retourner 404 quand aucun taux de change trouvé")
    void getSpecificRate_NoExchangeRateFound() throws Exception {
        // Given
        SearchHits<ExchangeRate> emptySearchHits = mock(SearchHits.class);
        when(emptySearchHits.hasSearchHits()).thenReturn(false);
        when(emptySearchHits.getTotalHits()).thenReturn(0L);

        when(elasticsearchOperations.search(any(Query.class), eq(ExchangeRate.class)))
            .thenReturn(emptySearchHits);

        // When & Then
        mockMvc.perform(get("/api/proxy/rate/EUR"))
            .andExpect(status().isNotFound());

        verify(elasticsearchOperations, times(1)).search(any(Query.class), eq(ExchangeRate.class));
    }

    @Test
    @DisplayName("GET /api/proxy/rate/{currency} - Devrait retourner 404 quand les rates sont null")
    void getSpecificRate_NullRates() throws Exception {
        // Given
        ExchangeRate exchangeRateWithoutRates = new ExchangeRate();
        exchangeRateWithoutRates.setId("test-id");
        exchangeRateWithoutRates.setBaseCurrency("USD");
        exchangeRateWithoutRates.setRates(null);

        SearchHit<ExchangeRate> mockHitWithoutRates = mock(SearchHit.class);
        when(mockHitWithoutRates.getContent()).thenReturn(exchangeRateWithoutRates);

        SearchHits<ExchangeRate> mockHitsWithoutRates = mock(SearchHits.class);
        when(mockHitsWithoutRates.hasSearchHits()).thenReturn(true);
        when(mockHitsWithoutRates.getSearchHit(0)).thenReturn(mockHitWithoutRates);
        when(mockHitsWithoutRates.getTotalHits()).thenReturn(1L);

        when(elasticsearchOperations.search(any(Query.class), eq(ExchangeRate.class)))
            .thenReturn(mockHitsWithoutRates);

        // When & Then
        mockMvc.perform(get("/api/proxy/rate/EUR"))
            .andExpect(status().isNotFound());

        verify(elasticsearchOperations, times(1)).search(any(Query.class), eq(ExchangeRate.class));
    }

    @Test
    @DisplayName("GET /api/proxy/rate/{currency} - Devrait gérer les erreurs Elasticsearch")
    void getSpecificRate_Error() throws Exception {
        // Given
        when(elasticsearchOperations.search(any(Query.class), eq(ExchangeRate.class)))
            .thenThrow(new RuntimeException("Erreur de connexion Elasticsearch"));

        // When & Then
        mockMvc.perform(get("/api/proxy/rate/EUR"))
            .andExpect(status().isInternalServerError());

        verify(elasticsearchOperations, times(1)).search(any(Query.class), eq(ExchangeRate.class));
    }

    @Test
    @DisplayName("Test des chemins d'API - Vérification des mappings")
    void testApiPaths() throws Exception {
        // Vérifier que tous les endpoints sont correctement mappés
        mockMvc.perform(get("/api/proxy/test"))
            .andExpect(status().isOk());

        // Test /api/proxy/simple avec nouveau mock
        SearchHit<ExchangeRate> mockHit1 = mock(SearchHit.class);
        when(mockHit1.getContent()).thenReturn(sampleExchangeRate);

        SearchHits<ExchangeRate> mockHits1 = mock(SearchHits.class);
        when(mockHits1.hasSearchHits()).thenReturn(true);
        when(mockHits1.getSearchHit(0)).thenReturn(mockHit1);
        when(mockHits1.getTotalHits()).thenReturn(1L);
        when(mockHits1.stream()).thenReturn(Arrays.asList(mockHit1).stream());

        when(elasticsearchOperations.search(any(Query.class), eq(ExchangeRate.class)))
            .thenReturn(mockHits1);

        mockMvc.perform(get("/api/proxy/simple"))
            .andExpect(status().isOk());

        // Test /api/proxy/latest-rates avec nouveau mock
        SearchHit<ExchangeRate> mockHit2 = mock(SearchHit.class);
        when(mockHit2.getContent()).thenReturn(sampleExchangeRate);

        SearchHits<ExchangeRate> mockHits2 = mock(SearchHits.class);
        when(mockHits2.hasSearchHits()).thenReturn(true);
        when(mockHits2.getSearchHit(0)).thenReturn(mockHit2);
        when(mockHits2.getTotalHits()).thenReturn(1L);

        reset(elasticsearchOperations);
        when(elasticsearchOperations.search(any(Query.class), eq(ExchangeRate.class)))
            .thenReturn(mockHits2);

        mockMvc.perform(get("/api/proxy/latest-rates"))
            .andExpect(status().isOk());

        // Test /api/proxy/all-rates avec nouveau mock
        SearchHit<ExchangeRate> mockHit3 = mock(SearchHit.class);
        when(mockHit3.getContent()).thenReturn(sampleExchangeRate);

        SearchHits<ExchangeRate> mockHits3 = mock(SearchHits.class);
        when(mockHits3.hasSearchHits()).thenReturn(true);
        when(mockHits3.getSearchHit(0)).thenReturn(mockHit3);
        when(mockHits3.getTotalHits()).thenReturn(1L);
        when(mockHits3.stream()).thenReturn(Arrays.asList(mockHit3).stream());

        reset(elasticsearchOperations);
        when(elasticsearchOperations.search(any(Query.class), eq(ExchangeRate.class)))
            .thenReturn(mockHits3);

        mockMvc.perform(get("/api/proxy/all-rates"))
            .andExpect(status().isOk());

        // Test /api/proxy/rate/XYZ avec nouveau mock
        SearchHit<ExchangeRate> mockHit4 = mock(SearchHit.class);
        when(mockHit4.getContent()).thenReturn(sampleExchangeRate);

        SearchHits<ExchangeRate> mockHits4 = mock(SearchHits.class);
        when(mockHits4.hasSearchHits()).thenReturn(true);
        when(mockHits4.getSearchHit(0)).thenReturn(mockHit4);
        when(mockHits4.getTotalHits()).thenReturn(1L);

        reset(elasticsearchOperations);
        when(elasticsearchOperations.search(any(Query.class), eq(ExchangeRate.class)))
            .thenReturn(mockHits4);

        // Test avec une devise qui n'existe pas dans les rates de test (EUR, GBP, JPY)
        mockMvc.perform(get("/api/proxy/rate/XYZ"))
            .andExpect(status().isNotFound()); // XYZ n'existe pas dans les rates
    }
} 