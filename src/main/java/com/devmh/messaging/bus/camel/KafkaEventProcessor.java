package com.devmh.messaging.bus.camel;

import com.devmh.messaging.events.AppEventType;
import com.devmh.messaging.events.CaseCreated;
import com.devmh.messaging.events.EventEnvelope;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.Exchange;
import org.apache.camel.Processor;
import org.apache.camel.component.kafka.KafkaConstants;
import org.springframework.stereotype.Component;

@Slf4j
@Component("kafkaEventProcessor")
public class KafkaEventProcessor implements Processor {

    @Override
    public void process(Exchange exchange) {
        CaseCreated caseCreated = exchange.getIn().getBody(CaseCreated.class);
        log.warn("Case created: {}", caseCreated);
        EventEnvelope env = EventEnvelope.of(AppEventType.CASE_CREATED.topic, caseCreated);
        exchange.getIn().setBody(env);
        exchange.getIn().setHeader(KafkaConstants.TOPIC, env.topic());
    }
}
