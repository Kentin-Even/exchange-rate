package com.learn.kafka.config;

import com.learn.kafka.model.ExchangeRate;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonSerializer;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Tests unitaires pour KafkaConfig")
class KafkaConfigTest {

    private KafkaConfig kafkaConfig;

    @BeforeEach
    void setUp() {
        kafkaConfig = new KafkaConfig();
        ReflectionTestUtils.setField(kafkaConfig, "bootstrapServers", "localhost:9092");
    }

    @Test
    @DisplayName("Test création du topic exchange-rates")
    void testExchangeRatesTopic() {
        // When
        NewTopic topic = kafkaConfig.exchangeRatesTopic();

        // Then
        assertThat(topic).isNotNull();
        assertThat(topic.name()).isEqualTo("exchange-rates");
        assertThat(topic.numPartitions()).isEqualTo(1);
        assertThat(topic.replicationFactor()).isEqualTo((short) 1);
    }

    @Test
    @DisplayName("Test création du ProducerFactory pour ExchangeRate")
    void testExchangeRateProducerFactory() {
        // When
        ProducerFactory<String, ExchangeRate> producerFactory = kafkaConfig.exchangeRateProducerFactory();

        // Then
        assertThat(producerFactory).isNotNull();
        assertThat(producerFactory.getConfigurationProperties())
            .containsEntry(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092")
            .containsEntry(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class)
            .containsEntry(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class);
    }

    @Test
    @DisplayName("Test création du KafkaTemplate pour ExchangeRate")
    void testExchangeRateKafkaTemplate() {
        // When
        KafkaTemplate<String, ExchangeRate> kafkaTemplate = kafkaConfig.exchangeRateKafkaTemplate();

        // Then
        assertThat(kafkaTemplate).isNotNull();
        assertThat(kafkaTemplate.getProducerFactory()).isNotNull();
    }

    @Test
    @DisplayName("Test création du ProducerFactory pour String")
    void testStringProducerFactory() {
        // When
        ProducerFactory<String, String> producerFactory = kafkaConfig.stringProducerFactory();

        // Then
        assertThat(producerFactory).isNotNull();
        assertThat(producerFactory.getConfigurationProperties())
            .containsEntry(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "localhost:9092")
            .containsEntry(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class)
            .containsEntry(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, StringSerializer.class);
    }

    @Test
    @DisplayName("Test création du KafkaTemplate pour String")
    void testStringKafkaTemplate() {
        // When
        KafkaTemplate<String, String> kafkaTemplate = kafkaConfig.stringKafkaTemplate();

        // Then
        assertThat(kafkaTemplate).isNotNull();
        assertThat(kafkaTemplate.getProducerFactory()).isNotNull();
    }

    @Test
    @DisplayName("Test avec un autre serveur bootstrap")
    void testWithDifferentBootstrapServer() {
        // Given
        ReflectionTestUtils.setField(kafkaConfig, "bootstrapServers", "another-server:9093");

        // When
        ProducerFactory<String, String> producerFactory = kafkaConfig.stringProducerFactory();

        // Then
        assertThat(producerFactory.getConfigurationProperties())
            .containsEntry(ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, "another-server:9093");
    }
} 