package com.devmh.messaging_starter.starter;

import com.devmh.messaging_starter.core.Envelope;
import com.devmh.messaging_starter.core.EventBusPublisher;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.Map;

@AutoConfiguration
@EnableKafka
@EnableConfigurationProperties(EventBusProperties.class)
public class KafkaAutoConfiguration {

    @Bean ProducerFactory<String,Object> ebProducerFactory(EventBusProperties p) {
        return new DefaultKafkaProducerFactory<>(Map.of(
                ProducerConfig.BOOTSTRAP_SERVERS_CONFIG, p.getBootstrapServers(),
                ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class,
                ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, JsonSerializer.class,
                JsonSerializer.ADD_TYPE_INFO_HEADERS, true
        ));
    }
    @Bean KafkaTemplate<String,Object> ebKafkaTemplate(ProducerFactory<String,Object> pf) {
        return new KafkaTemplate<>(pf);
    }

    @Bean @ConditionalOnMissingBean
    public EventBusPublisher eventBusPublisher(KafkaTemplate<String,Object> kafka) {
        return (topic, payload) -> kafka.send(topic, Envelope.of(topic, payload));
    }

    @Bean ConsumerFactory<String,Object> ebConsumerFactory(EventBusProperties p) {
        return new DefaultKafkaConsumerFactory<>(Map.of(
                ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, p.getBootstrapServers(),
                ConsumerConfig.GROUP_ID_CONFIG, p.getGroupId(),
                ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class,
                ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, JsonDeserializer.class,
                JsonDeserializer.TRUSTED_PACKAGES, "*",
                JsonDeserializer.USE_TYPE_INFO_HEADERS, true
        ));
    }

    @Bean ConcurrentKafkaListenerContainerFactory<String,Object> ebKafkaListenerFactory(ConsumerFactory<String,Object> cf) {
        var f = new ConcurrentKafkaListenerContainerFactory<String,Object>();
        f.setConsumerFactory(cf);
        return f;
    }
}
