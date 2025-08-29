package com.devmh.messaging_starter.core;

import java.time.Instant;

public record Envelope(String topic, String type, Instant timestamp, Object payload) {
    public static Envelope of(String topic, Object payload) {
        return new Envelope(topic, payload.getClass().getSimpleName(), Instant.now(), payload);
    }
}
