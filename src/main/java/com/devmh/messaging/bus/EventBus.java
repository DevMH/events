package com.devmh.messaging.bus;

import com.devmh.messaging.events.AppEventType;
import com.devmh.messaging.events.CaseCreated;
import com.devmh.messaging.events.EventEnvelope;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service
public class EventBus {

    private final SimpMessagingTemplate ws;
    private final KafkaTemplate<String, Object> kafka;

    // Kafka topics
    @Value("${app.kafka.topics.updated}")
    private String topicUpdated;

    @Value("${app.kafka.topics.created}")
    private String topicCreated;

    // Web Socket topic
    @Value("${app.ws.base:/topic}")
    private String wsBase;

    @KafkaListener(topicPattern = "case\\..*", containerFactory = "kafkaListenerContainerFactory")
    public void onEvent(String topic, EventEnvelope envelope) {
        AppEventType type = AppEventType.fromTopic(topic);
        String destination = type != null ? type.destination : wsBase + "/generic";
        log.info("Kafka to WebSocket: topic={}, type={}, destination={}", topic, envelope.type(), destination);
        ws.convertAndSend(destination, envelope);
    }

    @EventListener
    public void handleCaseCreated(CaseCreated event) {
        publish(topicCreated, event);
    }

    @EventListener
    public void handleCaseUpdated(CaseCreated event) {
        publish(topicUpdated, event);
    }

    private void publish(String topic, Object payload) {
        kafka.send(topic, EventEnvelope.of(topic, payload));
    }
}
