package com.devmh.messaging_starter.starter;

import com.devmh.messaging_starter.core.Envelope;
import com.devmh.messaging_starter.core.FanoutAuthorization;
import com.devmh.messaging_starter.core.SessionRegistry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Slf4j
@Component
class FanoutListener {
    private final EventBusProperties props;
    private final SimpMessagingTemplate ws;
    private final SessionRegistry sessions;
    private final FanoutAuthorization authz;

    FanoutListener(EventBusProperties p, SimpMessagingTemplate ws, SessionRegistry sr, FanoutAuthorization az) {
        this.props = p; this.ws = ws; this.sessions = sr; this.authz = az;
    }

    @KafkaListener(topicPattern = "case\\..*", containerFactory = "ebKafkaListenerFactory")
    public void onEvent(@org.springframework.messaging.handler.annotation.Header(org.springframework.kafka.support.KafkaHeaders.RECEIVED_TOPIC) String topic,
                        Envelope env) {
        String destination = props.getTopicPrefix() + "/" + topic.replace('.', '/');
        log.info("Fan out topic:{} -> {}", topic, destination);
        sessionsByUser().forEach((user, sessionIds) -> {
            java.security.Principal p = () -> user;
            if (authz.canReceive(p, env)) {
                ws.convertAndSendToUser(user, destination.replace(props.getTopicPrefix(), ""), env);
            }
        });
        // ws.convertAndSend(destination, env);
    }

    private java.util.Map<String, java.util.Set<String>> sessionsByUser() {
        try {
            var f = SessionRegistry.class.getDeclaredField("sessionsByUser");
            f.setAccessible(true);
            return (java.util.Map<String, java.util.Set<String>>) f.get(sessions);
        } catch (Exception e) {
            return java.util.Map.of();
        }
    }
}
