package com.devmh.messaging.bus;

import com.devmh.messaging.events.EventEnvelope;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.apache.camel.builder.RouteBuilder;
import org.apache.camel.component.jackson.JacksonDataFormat;
import org.apache.camel.component.kafka.KafkaConstants;
import org.apache.camel.model.dataformat.JsonLibrary;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;


@Component
@RequiredArgsConstructor
public class Routes extends RouteBuilder {

    @Value("${spring.kafka.bootstrap-servers}")
    String brokers;

    private final ObjectMapper objectMapper;

    @Override
    public void configure() {
        JacksonDataFormat jsonFormat = new JacksonDataFormat();
        jsonFormat.setObjectMapper(objectMapper);
/*
        from("seda:app-events")
                .routeId("app-events-to-kafka")
                .process(ex -> {
                    EventEnvelope env = ex.getIn().getBody(EventEnvelope.class);
                    ex.getIn().setHeader(KafkaConstants.TOPIC, env.topic());
                })
                .marshal(jsonFormat)
                // Use a dummy topic in URI; actual topic comes from header
                .toD("kafka:dummy?brokers=" + brokers);

        from("kafka:case.updated,case.created?" +
                "brokers=" + brokers + "&" +
                "groupId=${app.instance-id}&autoOffsetReset=latest")
                .routeId("kafka-to-ws-fanout")
                .unmarshal(jsonFormat)
                .to("log:com.devmh.messaging.camel?showAll=true")
                .process("wsMessageProcessor");
*/
    }
}