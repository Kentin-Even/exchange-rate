package com.learn.kafka.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ScheduledExchangeRateService {

    private final ExchangeRateService exchangeRateService;

    // Exécute toutes les 30 minutes (1800000 ms)
    @Scheduled(fixedRate = 1800000)
    public void fetchExchangeRatesAutomatically() {
        log.info("Starting scheduled fetch of exchange rates...");
        
        exchangeRateService.fetchAndPublishExchangeRates()
            .subscribe(
                exchangeRate -> log.info("Successfully fetched and published exchange rates for base currency: {}", 
                                       exchangeRate.getBaseCurrency()),
                error -> log.error("Error fetching exchange rates: {}", error.getMessage())
            );
    }

    // Pour test - exécute toutes les 2 minutes
    @Scheduled(fixedRate = 120000)
    public void fetchExchangeRatesForTesting() {
        log.info("Test fetch of exchange rates...");
        
        exchangeRateService.fetchAndPublishExchangeRates()
            .subscribe(
                exchangeRate -> log.info("Test fetch successful - ID: {}", exchangeRate.getId()),
                error -> log.error("Test fetch error: {}", error.getMessage())
            );
    }
} 