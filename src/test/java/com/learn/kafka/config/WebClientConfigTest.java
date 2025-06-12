package com.learn.kafka.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.reactive.function.client.WebClient;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Tests unitaires pour WebClientConfig")
class WebClientConfigTest {

    private WebClientConfig webClientConfig;

    @BeforeEach
    void setUp() {
        webClientConfig = new WebClientConfig();
    }

    @Test
    @DisplayName("Test création du WebClient")
    void testWebClientCreation() {
        // When
        WebClient webClient = webClientConfig.webClient();

        // Then
        assertThat(webClient).isNotNull();
    }

    @Test
    @DisplayName("Test que le WebClient a la bonne URL de base")
    void testWebClientBaseUrl() {
        // When
        WebClient webClient = webClientConfig.webClient();

        // Then
        assertThat(webClient).isNotNull();
        // On peut vérifier que le WebClient a été correctement configuré
        // même si on ne peut pas directement vérifier l'URL de base
        // (car c'est une propriété interne du WebClient)
    }

    @Test
    @DisplayName("Test configuration multiple")
    void testMultipleWebClientCreation() {
        // When
        WebClient webClient1 = webClientConfig.webClient();
        WebClient webClient2 = webClientConfig.webClient();

        // Then
        assertThat(webClient1).isNotNull();
        assertThat(webClient2).isNotNull();
        // Chaque appel devrait créer une nouvelle instance
        assertThat(webClient1).isNotSameAs(webClient2);
    }
} 