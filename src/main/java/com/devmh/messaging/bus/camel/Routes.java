package com.devmh.messaging.bus.camel;

import com.devmh.messaging.events.CaseCreated;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.camel.builder.RouteBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class Routes extends RouteBuilder {

    @Value("${spring.kafka.bootstrap-servers}")
    String brokers;

    @Override
    public void configure() {

        from("spring-event:appEventReceived")
                .routeId("app-events-to-kafka")
                .filter(exchange -> exchange.getIn().getBody(CaseCreated.class) != null)
                .process("kafkaEventProcessor")
                .toD("kafka:case.created?brokers=" + brokers
                        + "&valueSerializer=org.springframework.kafka.support.serializer.JsonSerializer");

        from("kafka:case.updated,case.created?" +
                "brokers=" + brokers + "&groupId=${app.instance-id}&autoOffsetReset=latest"
                + "&valueDeserializer=org.springframework.kafka.support.serializer.JsonDeserializer"
                + "&additionalProperties.spring.json.trusted.packages=*")
                .routeId("kafka-to-ws-fanout")
                .process("wsFanoutProcessor");
    }

}