package com.devmh.messaging.controller;

import com.devmh.messaging.events.CaseCreated;
import com.devmh.messaging.events.CaseUpdated;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Map;

@Slf4j
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/events")
public class EventController {

    private final ApplicationEventPublisher publisher;

    @PostMapping("/create/{caseId}")
    public void create(@PathVariable String caseId) {
        log.info("Controller received case created event request: {}", caseId);
        publisher.publishEvent(new CaseCreated(this, caseId, Instant.now()));
    }

    @PostMapping("/update/{caseId}")
    public void update(@PathVariable String caseId, @RequestBody Map<String, Object> changes) {
        log.info("Controller received case updated event request: {}", caseId);
        publisher.publishEvent(new CaseUpdated(this, caseId, Instant.now(), changes));
    }
}
