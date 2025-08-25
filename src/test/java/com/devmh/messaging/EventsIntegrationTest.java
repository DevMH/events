package com.devmh.messaging;

import com.devmh.messaging.events.CaseCreated;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.boot.test.web.client.TestRestTemplate;

import org.springframework.messaging.simp.stomp.*;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;
import org.springframework.web.socket.sockjs.client.SockJsClient;
import org.springframework.web.socket.sockjs.client.Transport;
import org.springframework.web.socket.sockjs.client.WebSocketTransport;

import java.lang.reflect.Type;
import java.util.*;
import java.util.concurrent.*;

import static org.assertj.core.api.Assertions.assertThat;

@Slf4j
@ExtendWith(SpringExtension.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@EmbeddedKafka(partitions = 1, topics = {
        "case.updated", "case.created"
})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
@Profile("test")
class EventsIntegrationTest {

    @LocalServerPort
    int port;

    @Autowired
    private ApplicationEventPublisher publisher;

    @Autowired
    TestRestTemplate rest;

    private WebSocketStompClient stompClient;

    @DynamicPropertySource
    static void kafkaProps(DynamicPropertyRegistry registry) {
        // Point the app under test at the embedded Kafka broker
        registry.add("spring.kafka.bootstrap-servers",
                () -> System.getProperty("spring.embedded.kafka.brokers"));
        // Ensure per-test unique consumer group
        registry.add("app.instance-id", () -> "test-" + UUID.randomUUID());
        // Keep logs quieter
        registry.add("logging.level.org.springframework.kafka", () -> "WARN");
    }

    @BeforeEach
    void setup() {
        List<Transport> transports = List.of(new WebSocketTransport(new StandardWebSocketClient()));
        SockJsClient sockJsClient = new SockJsClient(transports);
        stompClient = new WebSocketStompClient(sockJsClient);
        stompClient.setMessageConverter(new org.springframework.messaging.converter.StringMessageConverter());
        // Jackson is not required on client; we read frames as Strings
        stompClient.setInboundMessageSizeLimit(256 * 1024);
        log.info("Stomp Client created in setup");
    }

    @AfterEach
    void tearDown() {
        if (stompClient != null) {
            stompClient.stop();
            log.info("Stomp Client stopped in tearDown");
        }
    }

    private StompSession connect() throws Exception {
        String url = String.format("ws://localhost:%d/ws", port);
        StompSession fut = stompClient.connectAsync(url, new StompSessionHandlerAdapter() {}).get(10, TimeUnit.SECONDS);
        log.info("Stomp Client connected, returning session: {}", fut);
        return fut;
    }

    private static class CollectingHandler implements StompFrameHandler {
        private final BlockingQueue<String> frames = new LinkedBlockingQueue<>();
        private final CountDownLatch latch;

        CollectingHandler(int expected) {
            this.latch = new CountDownLatch(expected);
        }

        @Override public Type getPayloadType(StompHeaders headers) {
            return String.class;
        }

        @Override public void handleFrame(StompHeaders headers, Object payload) {
            log.info("Received frame {}", payload);
            frames.add(String.valueOf(payload));
            latch.countDown();
        }

        boolean await(long timeout, TimeUnit unit) throws InterruptedException {
            return latch.await(timeout, unit);
        }

        List<String> drainAll() {
            return new ArrayList<>(frames);
        }
    }

    private record Duo(CollectingHandler a, CollectingHandler b) {}

    private Duo subscribeTwo(StompSession s1, StompSession s2, String destination) {
        CollectingHandler h1 = new CollectingHandler(1);
        CollectingHandler h2 = new CollectingHandler(1);
        s1.subscribe(destination, h1);
        s2.subscribe(destination, h2);
        return new Duo(h1, h2);
    }

    @Test
    @Timeout(30)
    void shouldFanOutForInternalPublisher() throws Exception {
        // Two independent WS clients
        StompSession s1 = connect();
        StompSession s2 = connect();
        try {
            String dest = "/topic/case/created";
            Duo duo = subscribeTwo(s1, s2, dest);

            // Publish internally via the KafkaEventPublisher (goes through Kafka → listener → WS)
            publisher.publishEvent(new CaseCreated(this, "CASE-1", java.time.Instant.now()));

            // Both clients must receive
            boolean ok1 = duo.a.await(10, TimeUnit.SECONDS);
            boolean ok2 = duo.b.await(10, TimeUnit.SECONDS);
            assertThat(ok1).as("client 1 received frame").isTrue();
            assertThat(ok2).as("client 2 received frame").isTrue();

            // Optionally, assert body contains case id
            assertThat(duo.a.drainAll().toString()).contains("CASE-1");
            assertThat(duo.b.drainAll().toString()).contains("CASE-1");
        } finally {
            s1.disconnect();
            s2.disconnect();
        }
    }

    @Test
    @Timeout(30)
    void shouldFanOutForControllerPost() throws Exception {
        StompSession s1 = connect();
        StompSession s2 = connect();
        try {
            String dest = "/topic/case/created";
            Duo duo = subscribeTwo(s1, s2, dest);

            // Hit the REST controller (PublishController) which uses the publisher behind the scenes
            String url = String.format("http://localhost:%d/api/events/create/CASE-2", port);
            rest.postForEntity(url, null, Void.class);

            boolean ok1 = duo.a.await(10, TimeUnit.SECONDS);
            boolean ok2 = duo.b.await(10, TimeUnit.SECONDS);
            assertThat(ok1).as("client 1 received frame").isTrue();
            assertThat(ok2).as("client 2 received frame").isTrue();

            assertThat(duo.a.drainAll().toString()).contains("CASE-2");
            assertThat(duo.b.drainAll().toString()).contains("CASE-2");
        } finally {
            s1.disconnect();
            s2.disconnect();
        }
    }
}

