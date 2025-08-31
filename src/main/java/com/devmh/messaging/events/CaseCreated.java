package com.devmh.messaging.events;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.springframework.context.ApplicationEvent;

import java.time.Instant;

@Getter
@Setter
@ToString
@JsonIgnoreProperties("timestamp")
public class CaseCreated extends ApplicationEvent {

    private final String caseId;
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
    private final Instant created;

    public CaseCreated(Object source, String caseId, Instant created) {
        super(source);
        this.caseId = caseId;
        this.created = created;
    }
}
