package com.devmh.messaging.events;

import lombok.Getter;
import lombok.Setter;
import org.springframework.context.ApplicationEvent;

import java.time.Instant;
import java.util.Map;

@Getter
@Setter
public class CaseUpdated extends ApplicationEvent {

    private final String caseId;
    private final Instant createdAt;
    private final Instant updatedAt;
    private final Map<String, Object> payload;

    public CaseUpdated(Object source, String caseId, Instant updatedAt, Map<String, Object> changes) {
        super(source);
        this.caseId = caseId;
        this.createdAt = updatedAt;
        this.updatedAt = updatedAt;
        this.payload = changes;
    }
}
