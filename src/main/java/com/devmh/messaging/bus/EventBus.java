package com.devmh.messaging.bus;

import com.devmh.messaging.events.AppEventType;
import com.devmh.messaging.events.CaseCreated;
import com.devmh.messaging.events.CaseUpdated;
import com.devmh.messaging.events.EventEnvelope;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.event.EventListener;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import static org.springframework.kafka.support.KafkaHeaders.RECEIVED_TOPIC;

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
    public void onEvent(@Header(RECEIVED_TOPIC) String topic,
                        @Payload String envelope) {
        log.info("Event bus received Kafka event: {} from topic: {}", envelope, topic);
        log.info("Event bus Kafka topic: {}", topic);
        AppEventType type = AppEventType.fromTopic(topic);
        String destination = type != null ? type.destination : wsBase + "/generic";
        log.info("Event bus fanning out to subscribed WebSockets: topic={}, message={}, destination={}", topic, envelope, destination);
        //ws.convertAndSend(destination, envelope);
        ws.convertAndSend("/topic/case/created", envelope);
        //ws.convertAndSend("/topic/case/created", "test case created");
        log.info("Fan-out complete");
    }

    @EventListener
    public void handleCaseCreated(CaseCreated event) {
        log.info("Event bus received case created app event: {}", event);
        publish(topicCreated, event);
    }

    @EventListener
    public void handleCaseUpdated(CaseUpdated event) {
        log.info("Event bus received case updated app event: {}", event);
        publish(topicUpdated, event);
    }

    private void publish(String topic, Object payload) {
        log.info("Event bus publishing event: {} to Kafka topic: {}", payload, topic);
        kafka.send(topic, EventEnvelope.of(topic, payload).toString());
    }
}
