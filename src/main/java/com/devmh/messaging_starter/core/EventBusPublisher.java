package com.devmh.messaging_starter.core;

public interface EventBusPublisher {
    void publish(String topic, Object payload);
}
