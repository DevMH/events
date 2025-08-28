package com.devmh.messaging.bus;

import com.devmh.messaging.events.EventEnvelope;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.kafka.KafkaConstants;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;


@Component
public class Routes extends RouteBuilder {

    @Value("${spring.kafka.bootstrap-servers}")
    String brokers;


    @Override
    public void configure() {
        from("seda:app-events")
                .routeId("app-events-to-kafka")
                .process(ex -> {
                    EventEnvelope env = ex.getIn().getBody(EventEnvelope.class);
                    ex.getIn().setHeader(KafkaConstants.TOPIC, env.topic());
                })
                .marshal().json(JsonLibrary.Jackson)
                // Use a dummy topic in URI; actual topic comes from header
                .toD("kafka:dummy?brokers=" + brokers);

        from("kafka:case.locked,case.unlocked,case.updated,case.created?" +
                "brokers=" + "${spring.kafka.bootstrap-servers}" + "&" +
                "groupId=${app.instance-id}&autoOffsetReset=latest")
                .routeId("kafka-to-ws-fanout")
                .unmarshal().json(JsonLibrary.Jackson, EventEnvelope.class)
                .process("wsMessageProcessor");
    }
}