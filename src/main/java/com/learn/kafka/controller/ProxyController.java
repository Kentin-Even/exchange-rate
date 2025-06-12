package com.learn.kafka.controller;

import com.learn.kafka.model.ExchangeRate;
import com.learn.kafka.service.ElasticsearchService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.data.elasticsearch.core.query.Criteria;
import org.springframework.data.elasticsearch.core.query.CriteriaQuery;
import org.springframework.data.elasticsearch.core.query.Query;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/proxy")
@RequiredArgsConstructor
@Slf4j
public class ProxyController {

    private final ElasticsearchOperations elasticsearchOperations;
    
    /**
     * Endpoint de test pour vérifier que le contrôleur fonctionne
     */
    @GetMapping("/test")
    public ResponseEntity<String> test() {
        log.info("Test endpoint called");
        return ResponseEntity.ok("Proxy controller is working!");
    }
    
    /**
     * Endpoint simple qui récupère tous les documents sans filtre
     */
    @GetMapping("/simple")
    public ResponseEntity<List<ExchangeRate>> getSimpleData() {
        try {
            log.info("Fetching all data without filters...");
            
            SearchHits<ExchangeRate> searchHits = elasticsearchOperations.search(
                Query.findAll(), ExchangeRate.class);
            
            log.info("Found {} total records", searchHits.getTotalHits());
            
            List<ExchangeRate> rates = searchHits.stream()
                    .map(SearchHit::getContent)
                    .collect(Collectors.toList());
                    
            log.info("Returning {} records", rates.size());
            return ResponseEntity.ok(rates);
            
        } catch (Exception e) {
            log.error("Error in simple endpoint: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
    
    /**
     * Endpoint principal pour les équipes internes - récupère les derniers taux de change
     */
    @GetMapping("/latest-rates")
    public ResponseEntity<ExchangeRate> getLatestExchangeRates() {
        try {
            log.info("Fetching latest exchange rates from Elasticsearch...");
            
            // Requête pour récupérer le document le plus récent, trié par timestamp descendant
            CriteriaQuery query = new CriteriaQuery(Criteria.where("id").exists());
            query.addSort(Sort.by(Sort.Direction.DESC, "timestamp"));
            query.setMaxResults(1);
            
            SearchHits<ExchangeRate> searchHits = elasticsearchOperations.search(query, ExchangeRate.class);
            log.info("Found {} exchange rate records", searchHits.getTotalHits());
            
            if (searchHits.hasSearchHits()) {
                ExchangeRate latestRate = searchHits.getSearchHit(0).getContent();
                log.info("Returning latest exchange rate with ID: {}, baseCurrency: {} and timestamp: {}", 
                        latestRate.getId(), latestRate.getBaseCurrency(), latestRate.getTimestamp());
                return ResponseEntity.ok(latestRate);
            } else {
                log.warn("No exchange rates found in Elasticsearch");
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            log.error("Error fetching latest exchange rates: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Endpoint pour récupérer tous les taux de change disponibles
     */
    @GetMapping("/all-rates")
    public ResponseEntity<List<ExchangeRate>> getAllExchangeRates() {
        try {
            log.info("Fetching all exchange rates from Elasticsearch...");
            
            CriteriaQuery query = new CriteriaQuery(Criteria.where("id").exists());
            query.addSort(Sort.by(Sort.Direction.DESC, "timestamp"));
            query.setMaxResults(100);
            
            SearchHits<ExchangeRate> searchHits = elasticsearchOperations.search(query, ExchangeRate.class);
            log.info("Found {} exchange rate records", searchHits.getTotalHits());
            
            List<ExchangeRate> rates = searchHits.stream()
                    .map(SearchHit::getContent)
                    .collect(Collectors.toList());
            
            return ResponseEntity.ok(rates);
        } catch (Exception e) {
            log.error("Error fetching all exchange rates: {}", e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }

    /**
     * Endpoint pour récupérer un taux spécifique par devise
     */
    @GetMapping("/rate/{currency}")
    public ResponseEntity<Double> getSpecificRate(@PathVariable String currency) {
        try {
            log.info("Fetching rate for currency: {}", currency);
            
            CriteriaQuery query = new CriteriaQuery(Criteria.where("id").exists());
            query.addSort(Sort.by(Sort.Direction.DESC, "timestamp"));
            query.setMaxResults(1);
            
            SearchHits<ExchangeRate> searchHits = elasticsearchOperations.search(query, ExchangeRate.class);
            
            if (searchHits.hasSearchHits()) {
                ExchangeRate latestRate = searchHits.getSearchHit(0).getContent();
                if (latestRate.getRates() != null) {
                    Double rate = latestRate.getRates().get(currency.toUpperCase());
                    
                    if (rate != null) {
                        log.info("Found rate for {}: {} (timestamp: {})", currency, rate, latestRate.getTimestamp());
                        return ResponseEntity.ok(rate);
                    } else {
                        log.warn("Currency {} not found in rates", currency);
                        return ResponseEntity.notFound().build();
                    }
                } else {
                    log.warn("No rates data in exchange rate record");
                    return ResponseEntity.notFound().build();
                }
            } else {
                log.warn("No exchange rates found in Elasticsearch");
                return ResponseEntity.notFound().build();
            }
        } catch (Exception e) {
            log.error("Error fetching rate for currency {}: {}", currency, e.getMessage(), e);
            return ResponseEntity.internalServerError().build();
        }
    }
} 