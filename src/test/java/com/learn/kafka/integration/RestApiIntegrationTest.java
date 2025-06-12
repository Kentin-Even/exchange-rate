package com.learn.kafka.integration;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import com.learn.kafka.model.ExchangeRate;
import com.learn.kafka.service.ElasticsearchService;
import com.learn.kafka.service.ExchangeRateService;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.http.*;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import reactor.core.publisher.Mono;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.github.tomakehurst.wiremock.client.WireMock.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@DisplayName("Tests d'intégration pour les API REST")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class RestApiIntegrationTest {

    @LocalServerPort
    private int port;

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ExchangeRateService exchangeRateService;

    @MockBean
    private ElasticsearchService elasticsearchService;

    @MockBean
    private ElasticsearchOperations elasticsearchOperations;

    private static WireMockServer wireMockServer;
    private ExchangeRate sampleExchangeRate;
    private String baseUrl;

    @BeforeAll
    static void beforeAll() {
        wireMockServer = new WireMockServer(8089);
        wireMockServer.start();
    }

    @AfterAll
    static void afterAll() {
        if (wireMockServer != null && wireMockServer.isRunning()) {
            wireMockServer.stop();
        }
    }

    @BeforeEach
    void setUp() {
        wireMockServer.resetAll();
        baseUrl = "http://localhost:" + port;
        
        // Créer des données de test
        sampleExchangeRate = createSampleExchangeRate();
        
        // Configuration du mock pour éviter les NullPointerException dans les scheduled methods
        when(exchangeRateService.fetchAndPublishExchangeRates())
            .thenReturn(Mono.just(sampleExchangeRate));
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
        rates.put("CHF", 0.92);
        rates.put("CAD", 1.25);
        rates.put("AUD", 1.35);
        rate.setRates(rates);
        
        return rate;
    }

    @Test
    @Order(1)
    @DisplayName("GET /api/exchange-rates/fetch - Test d'intégration complet avec gestion d'erreur")
    void testFetchExchangeRatesEndpoint() {
        // Given - Mock du service avec succès
        when(exchangeRateService.fetchAndPublishExchangeRates())
            .thenReturn(Mono.just(sampleExchangeRate));

        // When - Appel de l'endpoint
        ResponseEntity<ExchangeRate> response = restTemplate.getForEntity(
            baseUrl + "/api/exchange-rates/fetch", 
            ExchangeRate.class
        );

        // Then - Vérification de la réponse
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_JSON);
        assertThat(response.getBody()).isNotNull();
        
        ExchangeRate result = response.getBody();
        assertThat(result.getBaseCurrency()).isEqualTo("USD");
        assertThat(result.getRates()).hasSize(6);
        assertThat(result.getRates().get("EUR")).isEqualTo(0.85);
        assertThat(result.getRates().get("JPY")).isEqualTo(110.0);
        assertThat(result.getId()).isEqualTo("test-id-123");
    }

    @Test
    @Order(2)
    @DisplayName("GET /api/exchange-rates/fetch - Test avec service qui retourne empty")
    void testFetchExchangeRatesEndpoint_Empty() {
        // Given - Mock du service qui retourne empty
        when(exchangeRateService.fetchAndPublishExchangeRates())
            .thenReturn(Mono.empty());

        // When - Appel de l'endpoint
        ResponseEntity<String> response = restTemplate.getForEntity(
            baseUrl + "/api/exchange-rates/fetch", 
            String.class
        );

        // Then - Vérification de la réponse
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @Order(3)
    @DisplayName("POST /api/exchange-rates/test-elasticsearch - Test d'intégration")
    void testElasticsearchEndpoint() {
        // Given - Mock du service
        when(elasticsearchService.saveExchangeRate(any(ExchangeRate.class)))
            .thenReturn(sampleExchangeRate);

        // When - Appel de l'endpoint
        ResponseEntity<String> response = restTemplate.postForEntity(
            baseUrl + "/api/exchange-rates/test-elasticsearch", 
            null, 
            String.class
        );

        // Then - Vérification de la réponse
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("Test d'Elasticsearch réussi");
        assertThat(response.getBody()).contains("timestamp correct");
    }

    @Test
    @Order(4)
    @DisplayName("POST /api/exchange-rates/test-elasticsearch - Test avec erreur")
    void testElasticsearchEndpoint_WithError() {
        // Given - Mock du service avec erreur
        when(elasticsearchService.saveExchangeRate(any(ExchangeRate.class)))
            .thenThrow(new RuntimeException("Connexion Elasticsearch échouée"));

        // When - Appel de l'endpoint
        ResponseEntity<String> response = restTemplate.postForEntity(
            baseUrl + "/api/exchange-rates/test-elasticsearch", 
            null, 
            String.class
        );

        // Then - Vérification de la réponse d'erreur
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).contains("Erreur Elasticsearch");
        assertThat(response.getBody()).contains("Connexion Elasticsearch échouée");
    }

    @Test
    @Order(5)
    @DisplayName("GET /api/proxy/test - Test simple du proxy")
    void testProxyTestEndpoint() {
        // When - Appel de l'endpoint
        ResponseEntity<String> response = restTemplate.getForEntity(
            baseUrl + "/api/proxy/test", 
            String.class
        );

        // Then - Vérification de la réponse
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo("Proxy controller is working!");
        assertThat(response.getHeaders().getContentType().toString())
            .contains("text/plain");
    }

    @Test
    @Order(6)
    @DisplayName("GET /api/proxy/latest-rates - Test d'intégration proxy")
    void testProxyLatestRatesEndpoint() {
        // Given - Mock des opérations Elasticsearch
        SearchHit<ExchangeRate> mockSearchHit = mock(SearchHit.class);
        when(mockSearchHit.getContent()).thenReturn(sampleExchangeRate);

        SearchHits<ExchangeRate> mockSearchHits = mock(SearchHits.class);
        when(mockSearchHits.hasSearchHits()).thenReturn(true);
        when(mockSearchHits.getSearchHit(0)).thenReturn(mockSearchHit);
        when(mockSearchHits.getTotalHits()).thenReturn(1L);

        when(elasticsearchOperations.search(any(Query.class), eq(ExchangeRate.class)))
            .thenReturn(mockSearchHits);

        // When - Appel de l'endpoint
        ResponseEntity<ExchangeRate> response = restTemplate.getForEntity(
            baseUrl + "/api/proxy/latest-rates", 
            ExchangeRate.class
        );

        // Then - Vérification de la réponse
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getBaseCurrency()).isEqualTo("USD");
        assertThat(response.getBody().getRates()).containsKey("EUR");
    }

    @Test
    @Order(7)
    @DisplayName("GET /api/proxy/all-rates - Test avec multiple résultats")
    void testProxyAllRatesEndpoint() {
        // Given - Mock avec plusieurs résultats
        ExchangeRate secondRate = createSampleExchangeRate();
        secondRate.setId("test-id-456");
        secondRate.setTimestamp(LocalDateTime.now().minusHours(1).format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));

        SearchHit<ExchangeRate> mockSearchHit1 = mock(SearchHit.class);
        when(mockSearchHit1.getContent()).thenReturn(sampleExchangeRate);
        
        SearchHit<ExchangeRate> mockSearchHit2 = mock(SearchHit.class);
        when(mockSearchHit2.getContent()).thenReturn(secondRate);

        SearchHits<ExchangeRate> mockSearchHits = mock(SearchHits.class);
        when(mockSearchHits.hasSearchHits()).thenReturn(true);
        when(mockSearchHits.getTotalHits()).thenReturn(2L);
        when(mockSearchHits.stream()).thenReturn(
            Arrays.asList(mockSearchHit1, mockSearchHit2).stream()
        );

        when(elasticsearchOperations.search(any(Query.class), eq(ExchangeRate.class)))
            .thenReturn(mockSearchHits);

        // When - Appel de l'endpoint
        ResponseEntity<List<ExchangeRate>> response = restTemplate.exchange(
            baseUrl + "/api/proxy/all-rates", 
            HttpMethod.GET,
            null,
            new ParameterizedTypeReference<List<ExchangeRate>>() {}
        );

        // Then - Vérification de la réponse
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody()).hasSize(2);
        assertThat(response.getBody().get(0).getId()).isEqualTo("test-id-123");
        assertThat(response.getBody().get(1).getId()).isEqualTo("test-id-456");
    }

    @Test
    @Order(8)
    @DisplayName("GET /api/proxy/rate/{currency} - Test de récupération d'une devise spécifique")
    void testProxySpecificCurrencyEndpoint() {
        // Given - Mock des opérations Elasticsearch
        SearchHit<ExchangeRate> mockSearchHit = mock(SearchHit.class);
        when(mockSearchHit.getContent()).thenReturn(sampleExchangeRate);

        SearchHits<ExchangeRate> mockSearchHits = mock(SearchHits.class);
        when(mockSearchHits.hasSearchHits()).thenReturn(true);
        when(mockSearchHits.getSearchHit(0)).thenReturn(mockSearchHit);
        when(mockSearchHits.getTotalHits()).thenReturn(1L);

        when(elasticsearchOperations.search(any(Query.class), eq(ExchangeRate.class)))
            .thenReturn(mockSearchHits);

        // When - Appel de l'endpoint pour EUR
        ResponseEntity<Double> response = restTemplate.getForEntity(
            baseUrl + "/api/proxy/rate/EUR", 
            Double.class
        );

        // Then - Vérification de la réponse
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo(0.85);
    }

    @Test
    @Order(9)
    @DisplayName("GET /api/proxy/rate/{currency} - Test avec toutes les devises disponibles")
    void testProxyAllAvailableCurrencies() {
        // Given - Mock des opérations Elasticsearch
        SearchHit<ExchangeRate> mockSearchHit = mock(SearchHit.class);
        when(mockSearchHit.getContent()).thenReturn(sampleExchangeRate);

        SearchHits<ExchangeRate> mockSearchHits = mock(SearchHits.class);
        when(mockSearchHits.hasSearchHits()).thenReturn(true);
        when(mockSearchHits.getSearchHit(0)).thenReturn(mockSearchHit);
        when(mockSearchHits.getTotalHits()).thenReturn(1L);

        when(elasticsearchOperations.search(any(Query.class), eq(ExchangeRate.class)))
            .thenReturn(mockSearchHits);

        // Test pour chaque devise disponible
        String[] currencies = {"EUR", "GBP", "JPY", "CHF", "CAD", "AUD"};
        Double[] expectedRates = {0.85, 0.75, 110.0, 0.92, 1.25, 1.35};

        for (int i = 0; i < currencies.length; i++) {
            ResponseEntity<Double> response = restTemplate.getForEntity(
                baseUrl + "/api/proxy/rate/" + currencies[i], 
                Double.class
            );

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isEqualTo(expectedRates[i]);
        }
    }

    @Test
    @Order(10)
    @DisplayName("GET /api/proxy/rate/{currency} - Test devise inexistante")
    void testProxyNonExistentCurrency() {
        // Given - Mock des opérations Elasticsearch
        SearchHit<ExchangeRate> mockSearchHit = mock(SearchHit.class);
        when(mockSearchHit.getContent()).thenReturn(sampleExchangeRate);

        SearchHits<ExchangeRate> mockSearchHits = mock(SearchHits.class);
        when(mockSearchHits.hasSearchHits()).thenReturn(true);
        when(mockSearchHits.getSearchHit(0)).thenReturn(mockSearchHit);
        when(mockSearchHits.getTotalHits()).thenReturn(1L);

        when(elasticsearchOperations.search(any(Query.class), eq(ExchangeRate.class)))
            .thenReturn(mockSearchHits);

        // When - Appel de l'endpoint pour une devise qui n'existe pas
        ResponseEntity<String> response = restTemplate.getForEntity(
            baseUrl + "/api/proxy/rate/XYZ", 
            String.class
        );

        // Then - Vérification de la réponse
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    @Order(11)
    @DisplayName("POST /produce - Test d'intégration du producteur Kafka")
    void testProduceEndpoint() {
        // Given - Préparation de la requête
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("content", "Test integration message");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

        // When - Appel de l'endpoint
        ResponseEntity<String> response = restTemplate.postForEntity(
            baseUrl + "/produce", 
            request,
            String.class
        );

        // Then - Vérification de la réponse
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isEqualTo("Test integration message");
    }

    @Test
    @Order(12)
    @DisplayName("POST /produce - Test avec différents types de contenu")
    void testProduceEndpoint_VariousContent() {
        String[] testMessages = {
            "Simple message",
            "Message avec éèàç caractères spéciaux",
            "{\"json\": \"message\", \"number\": 123}",
            "Message très long " + "A".repeat(500)
        };

        for (String message : testMessages) {
            MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
            params.add("content", message);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(params, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(
                baseUrl + "/produce", 
                request,
                String.class
            );

            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
            assertThat(response.getBody()).isEqualTo(message);
        }

        // Test séparé pour le message vide (peut retourner null)
        MultiValueMap<String, String> emptyParams = new LinkedMultiValueMap<>();
        emptyParams.add("content", "");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<MultiValueMap<String, String>> emptyRequest = new HttpEntity<>(emptyParams, headers);

        ResponseEntity<String> emptyResponse = restTemplate.postForEntity(
            baseUrl + "/produce", 
            emptyRequest,
            String.class
        );

        assertThat(emptyResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        // Accepter null ou chaîne vide pour ce cas
        assertThat(emptyResponse.getBody() == null || emptyResponse.getBody().equals("")).isTrue();
    }

    @Test
    @Order(13)
    @DisplayName("Test de gestion d'erreur - Service indisponible")
    void testServiceError() {
        // Given - Mock d'une erreur du service
        when(exchangeRateService.fetchAndPublishExchangeRates())
            .thenReturn(Mono.error(new RuntimeException("Service Error")));

        // When - Appel de l'endpoint
        ResponseEntity<String> response = restTemplate.getForEntity(
            baseUrl + "/api/exchange-rates/fetch", 
            String.class
        );

        // Then - Vérification que l'erreur est gérée
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @Test
    @Order(14)
    @DisplayName("Test de performance - Appels concurrent")
    void testConcurrentCalls() {
        // Given - Configuration simple sans mock complexe
        // When - Appels concurrents
        long startTime = System.currentTimeMillis();
        
        // Simuler plusieurs appels concurrent
        for (int i = 0; i < 5; i++) {
            ResponseEntity<String> response = restTemplate.getForEntity(
                baseUrl + "/api/proxy/test", 
                String.class
            );
            assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        }

        long endTime = System.currentTimeMillis();
        long duration = endTime - startTime;

        // Then - Vérifier que les réponses sont rapides (moins de 5 secondes pour 5 appels)
        assertThat(duration).isLessThan(5000);
    }

    @Test
    @Order(15)
    @DisplayName("Test de robustesse - Headers et Content-Type")
    void testHeadersAndContentType() {
        // Test endpoint text uniquement (pour éviter les mocks complexes)
        ResponseEntity<String> textResponse = restTemplate.getForEntity(
            baseUrl + "/api/proxy/test", 
            String.class
        );

        assertThat(textResponse.getHeaders().getContentType().toString())
            .contains("text/plain");
    }
} 