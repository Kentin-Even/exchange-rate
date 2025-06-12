package com.learn.kafka;

import com.learn.kafka.producer.MessageProducer;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProducerController.class)
@TestPropertySource(properties = {"spring.kafka.topic-name=test-topic"})
@DisplayName("Tests unitaires pour ProducerController")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class ProducerControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private MessageProducer messageProducer;

    @Test
    @DisplayName("POST /produce - Devrait envoyer un message avec succès")
    void sendMessage_Success() throws Exception {
        // Given
        String testContent = "Test message content";
        
        // When & Then
        mockMvc.perform(post("/produce")
                .param("content", testContent))
            .andExpect(status().isOk())
            .andExpect(content().contentType(MediaType.TEXT_PLAIN_VALUE + ";charset=UTF-8"))
            .andExpect(content().string(testContent));

        // Vérifier que le message a été envoyé via le producer
        verify(messageProducer, times(1)).sendMessage("test-topic", testContent);
    }

    @Test
    @DisplayName("POST /produce - Devrait gérer les messages vides")
    void sendMessage_EmptyContent() throws Exception {
        // Given
        String emptyContent = "";
        
        // When & Then  
        mockMvc.perform(post("/produce")
                .param("content", emptyContent))
            .andExpect(status().isOk())
            .andExpect(content().string(emptyContent));

        verify(messageProducer, times(1)).sendMessage("test-topic", emptyContent);
    }

    @Test
    @DisplayName("POST /produce - Devrait gérer les messages avec caractères spéciaux")
    void sendMessage_SpecialCharacters() throws Exception {
        // Given
        String specialContent = "Message avec éè àù çç @#$%^&*()";
        
        // When & Then
        mockMvc.perform(post("/produce")
                .param("content", specialContent))
            .andExpect(status().isOk())
            .andExpect(content().string(specialContent));

        verify(messageProducer, times(1)).sendMessage("test-topic", specialContent);
    }

    @Test
    @DisplayName("POST /produce - Devrait gérer les messages JSON")
    void sendMessage_JsonContent() throws Exception {
        // Given
        String jsonContent = "{\"key\":\"value\",\"number\":123}";
        
        // When & Then
        mockMvc.perform(post("/produce")
                .param("content", jsonContent))
            .andExpect(status().isOk())
            .andExpect(content().string(jsonContent));

        verify(messageProducer, times(1)).sendMessage("test-topic", jsonContent);
    }

    @Test
    @DisplayName("POST /produce - Devrait gérer les longs messages")
    void sendMessage_LongContent() throws Exception {
        // Given
        String longContent = "A".repeat(1000); // Message de 1000 caractères
        
        // When & Then
        mockMvc.perform(post("/produce")
                .param("content", longContent))
            .andExpect(status().isOk())
            .andExpect(content().string(longContent));

        verify(messageProducer, times(1)).sendMessage("test-topic", longContent);
    }

    @Test
    @DisplayName("POST /produce - Devrait retourner 400 quand le paramètre content est manquant")
    void sendMessage_MissingContentParameter() throws Exception {
        // When & Then
        mockMvc.perform(post("/produce"))
            .andExpect(status().isBadRequest());

        // Vérifier qu'aucun message n'a été envoyé
        verify(messageProducer, never()).sendMessage(anyString(), anyString());
    }

    @Test
    @DisplayName("POST /produce - Devrait gérer les erreurs du producer Kafka")
    void sendMessage_ProducerError() throws Exception {
        // Given
        String testContent = "Test message";
        doThrow(new RuntimeException("Kafka connection failed"))
            .when(messageProducer).sendMessage(anyString(), anyString());
        
        // When & Then
        mockMvc.perform(post("/produce")
                .param("content", testContent))
            .andExpect(status().is5xxServerError());

        verify(messageProducer, times(1)).sendMessage("test-topic", testContent);
    }

    @Test
    @DisplayName("POST /produce - Devrait accepter les requêtes avec Content-Type application/x-www-form-urlencoded")
    void sendMessage_FormUrlEncoded() throws Exception {
        // Given
        String testContent = "Test form data";
        
        // When & Then
        mockMvc.perform(post("/produce")
                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                .param("content", testContent))
            .andExpect(status().isOk())
            .andExpect(content().string(testContent));

        verify(messageProducer, times(1)).sendMessage("test-topic", testContent);
    }

    @Test
    @DisplayName("Méthodes HTTP - Seul POST devrait être autorisé")
    void testHttpMethods() throws Exception {
        // GET ne devrait pas être autorisé
        mockMvc.perform(get("/produce"))
            .andExpect(status().isMethodNotAllowed());

        // PUT ne devrait pas être autorisé
        mockMvc.perform(put("/produce"))
            .andExpect(status().isMethodNotAllowed());

        // DELETE ne devrait pas être autorisé
        mockMvc.perform(delete("/produce"))
            .andExpect(status().isMethodNotAllowed());

        // Vérifier qu'aucun message n'a été envoyé pour ces méthodes
        verify(messageProducer, never()).sendMessage(anyString(), anyString());
    }

    @Test
    @DisplayName("Test de l'injection des dépendances - Topic name configuration")
    void testTopicNameInjection() throws Exception {
        // Ce test vérifie indirectement que la configuration du topic fonctionne
        String testContent = "Test topic injection";
        
        mockMvc.perform(post("/produce")
                .param("content", testContent))
            .andExpect(status().isOk());

        // Vérifier que le bon nom de topic est utilisé (configuré dans @TestPropertySource)
        verify(messageProducer, times(1)).sendMessage("test-topic", testContent);
        verify(messageProducer, never()).sendMessage(eq("wrong-topic"), anyString());
    }
} 