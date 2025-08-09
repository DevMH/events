package com.devmh.messaging.events;

import lombok.Getter;
import lombok.Setter;
import org.springframework.context.ApplicationEvent;

import java.time.Instant;

@Getter
@Setter
public class CaseCreated extends ApplicationEvent {

    private final String caseId;
    private final Instant created;

    public CaseCreated(Object source, String caseId, Instant created) {
        super(source);
        this.caseId = caseId;
        this.created = created;
    }
}
