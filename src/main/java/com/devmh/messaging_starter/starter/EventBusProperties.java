package com.devmh.messaging_starter.starter;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import java.util.Map;

@ConfigurationProperties(prefix = "eventbus")
@Data
public class EventBusProperties {
    private String bootstrapServers = "localhost:9092";
    private String groupId = "${HOSTNAME:local}-${random.uuid}";
    private Map<String,String> topics = Map.of(
            "created", "case.created",
            "updated", "case.updated"
    );
    private String wsEndpoint = "/ws";
    private boolean sockJs = true;
    private String topicPrefix = "/topic";
    private String appPrefix = "/app";
    private String userPrefix = "/user";
}