package com.devmh.messaging.events;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.datatype.jsr310.ser.InstantSerializer;

import java.time.Instant;

public record EventEnvelope(
        String topic,
        String type,
        @JsonSerialize(using = InstantSerializer.class)
        @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
        Instant timestamp,
        Object payload
) {
    public static EventEnvelope of(String topic, Object payload) {
        return new EventEnvelope(topic, payload.getClass().getSimpleName(), Instant.now(), payload);
    }
}