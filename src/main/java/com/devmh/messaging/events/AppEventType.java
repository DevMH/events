package com.devmh.messaging.events;

import java.util.Map;

public enum AppEventType {
    CASE_UPDATED("case.updated", "/topic/case/updated"),
    CASE_CREATED("case.created", "/topic/case/created");

    private static final Map<String, AppEventType> BY_TOPIC = Map.of(
            "case.updated", CASE_UPDATED,
            "case.created", CASE_CREATED
    );

    public final String topic;
    public final String destination;

    AppEventType(String topic, String destination) {
        this.topic = topic;
        this.destination = destination;
    }

    public static AppEventType fromTopic(String topic) {
        return BY_TOPIC.get(topic);
    }
}
