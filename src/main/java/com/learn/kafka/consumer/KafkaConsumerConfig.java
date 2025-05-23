package com.learn.kafka.consumer;

import com.learn.kafka.model.ExchangeRate;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class KafkaConsumerConfig {

  @Value("${spring.kafka.bootstrap-servers}")
  String bootstrapServers;

  @Value("${spring.kafka.consumer.group-id}")
  String consumerGroupId;

  @Bean
  public ConsumerFactory<String, String> consumerFactory() {
    Map<String, Object> configProps = new HashMap<>();
    configProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG,bootstrapServers);
    configProps.put(ConsumerConfig.GROUP_ID_CONFIG, consumerGroupId);
    configProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
    configProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
    return new DefaultKafkaConsumerFactory<>(configProps);
  }

  @Bean
  public ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory() {
    ConcurrentKafkaListenerContainerFactory<String, String> factory = new ConcurrentKafkaListenerContainerFactory<>();
    factory.setConsumerFactory(consumerFactory());
    return factory;
  }

  // Configuration pour les objets ExchangeRate
  @Bean
  public ConsumerFactory<String, ExchangeRate> exchangeRateConsumerFactory() {
    Map<String, Object> configProps = new HashMap<>();
    configProps.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
    configProps.put(ConsumerConfig.GROUP_ID_CONFIG, "exchange-rate-group");
    configProps.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
    configProps.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class);
    configProps.put(JsonDeserializer.TRUSTED_PACKAGES, "*");
    configProps.put(JsonDeserializer.VALUE_DEFAULT_TYPE, ExchangeRate.class.getName());
    return new DefaultKafkaConsumerFactory<>(configProps);
  }

  @Bean
  public ConcurrentKafkaListenerContainerFactory<String, ExchangeRate> exchangeRateKafkaListenerContainerFactory() {
    ConcurrentKafkaListenerContainerFactory<String, ExchangeRate> factory = new ConcurrentKafkaListenerContainerFactory<>();
    factory.setConsumerFactory(exchangeRateConsumerFactory());
    return factory;
  }

}
