package com.devmh.messaging.bus;

import com.devmh.messaging.events.AppEventType;
import com.devmh.messaging.events.EventEnvelope;
import org.apache.camel.*;
import org.apache.camel.component.kafka.KafkaConstants;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.user.SimpUser;
import org.springframework.messaging.simp.user.SimpUserRegistry;
import org.springframework.stereotype.Component;
import java.security.Principal;

@Component("wsMessageProcessor")
public class WsMessageProcessor implements Processor {
    private final SimpMessagingTemplate ws;
    private final SimpUserRegistry userRegistry;

    public WsMessageProcessor(SimpMessagingTemplate ws, SimpUserRegistry reg) {
        this.ws = ws;
        this.userRegistry = reg;
    }

    @Override
    public void process(Exchange exchange) {
        EventEnvelope env = exchange.getIn().getBody(EventEnvelope.class);
        String topic = exchange.getIn().getHeader(KafkaConstants.TOPIC, String.class);
        AppEventType type = AppEventType.fromTopic(topic);

        for (SimpUser user : userRegistry.getUsers()) {
            Principal p = user.getPrincipal();
            /*
            Authentication auth = (p instanceof Authentication a) ? a : null;
            if (authz.canReceive(auth, env)) {
                ws.convertAndSendToUser(p.getName(), userDest.replace("/user", ""), env);
            }
             */
        }
    }
}
