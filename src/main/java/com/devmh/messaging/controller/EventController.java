package com.devmh.messaging.controller;

import com.devmh.messaging.events.CaseCreated;
import com.devmh.messaging.events.CaseUpdated;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/events")
public class EventController {

    private final ApplicationEventPublisher publisher;

    @PostMapping("/create/{caseId}")
    public void create(@PathVariable String caseId) {
        publisher.publishEvent(new CaseCreated(this, caseId, Instant.now()));
    }

    @PostMapping("/update/{caseId}")
    public void update(@PathVariable String caseId, @RequestBody Map<String, Object> changes) {
        publisher.publishEvent(new CaseUpdated(this, caseId, Instant.now(), changes));
    }
}
