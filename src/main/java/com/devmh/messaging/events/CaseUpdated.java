package com.devmh.messaging.events;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;
import org.springframework.context.ApplicationEvent;

import java.time.Instant;
import java.util.Map;

@Getter
@Setter
@JsonIgnoreProperties("timestamp")
public class CaseUpdated extends ApplicationEvent {

    private final String caseId;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private final Instant createdAt;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
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
