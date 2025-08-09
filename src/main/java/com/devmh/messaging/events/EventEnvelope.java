package com.devmh.messaging.events;

import java.time.Instant;

public record EventEnvelope(
        String topic,
        String type,
        Instant timestamp,
        Object payload
) {
    public static EventEnvelope of(String topic, Object payload) {
        return new EventEnvelope(topic, payload.getClass().getSimpleName(), Instant.now(), payload);
    }
}