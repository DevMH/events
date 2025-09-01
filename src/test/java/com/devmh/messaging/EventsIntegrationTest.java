package com.devmh.messaging;

import com.devmh.messaging.events.CaseUpdated;
import com.devmh.messaging.events.EventEnvelope;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.NonNull;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.annotation.Profile;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
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

    @Autowired
    private ObjectMapper objectMapper;

    private WebSocketStompClient stompClient;

    @DynamicPropertySource
    static void kafkaProps(DynamicPropertyRegistry registry) {
        registry.add("spring.kafka.bootstrap-servers",
                () -> System.getProperty("spring.embedded.kafka.brokers"));
        registry.add("app.instance-id", () -> "test-" + UUID.randomUUID());
        registry.add("logging.level.org.springframework.kafka", () -> "WARN");
    }

    @BeforeEach
    void setup() {
        List<Transport> transports = List.of(new WebSocketTransport(new StandardWebSocketClient()));
        SockJsClient sockJsClient = new SockJsClient(transports);
        stompClient = new WebSocketStompClient(sockJsClient);
        stompClient.setMessageConverter(new MappingJackson2MessageConverter(objectMapper));
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
        String url = String.format("http://localhost:%d/ws", port);
        return stompClient.connectAsync(url, new StompSessionHandlerAdapter() {
            @Override
            public void handleException(@NonNull StompSession session, StompCommand command,
                                        @NonNull StompHeaders headers, byte[] payload, @NonNull Throwable exception) {
                throw new RuntimeException("Failure in WebSocket handling", exception);
            }
        })
        .get(10, TimeUnit.SECONDS);
    }

    private static class CollectingHandler implements StompFrameHandler {
        private final BlockingQueue<String> frames = new LinkedBlockingQueue<>();
        private final CountDownLatch latch;

        CollectingHandler(int expected) {
            this.latch = new CountDownLatch(expected);
        }

        @Override @NonNull public Type getPayloadType(@NonNull StompHeaders headers) {
            return EventEnvelope.class;
        }

        @Override public void handleFrame(@NonNull StompHeaders headers, Object payload) {
            log.info("Received frame {}", payload);
            frames.add(String.valueOf(payload));
            latch.countDown();
        }

        boolean await() throws InterruptedException {
            return latch.await(5, TimeUnit.SECONDS);
        }

        List<String> drainAll() {
            return new ArrayList<>(frames);
        }
    }

    private record Duo(CollectingHandler a, CollectingHandler b) {}

    @SneakyThrows
    private Duo subscribeTwo(StompSession s1, StompSession s2, String destination, int expected) {

        CollectingHandler h1 = new CollectingHandler(expected); // ws consumer 1
        CollectingHandler h2 = new CollectingHandler(expected); // ws consumer 2

        s1.subscribe(destination, h1);
        s2.subscribe(destination, h2);

        return new Duo(h1, h2);
    }

    @Test
    @Disabled
    @Timeout(15)
    void shouldFanOutForInternalPublisher() throws Exception {
        // Two independent WS clients
        StompSession s1 = connect();
        StompSession s2 = connect();
        try {
            String dest = "/topic/case/updated";
            Duo duo = subscribeTwo(s1, s2, dest, 2); // 1 from EventBus, 1 from Caml route

            // Publish internally via the KafkaEventPublisher (goes through Kafka → listener → WS)
            publisher.publishEvent(new CaseUpdated(this, "CASE-1", java.time.Instant.now(), Map.of("testProperty","updatedValue")));

            // Both clients must receive
            boolean ok1 = duo.a.await();
            boolean ok2 = duo.b.await();
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
    @Timeout(15)
    void shouldFanOutForControllerPost() throws Exception {
        StompSession s1 = connect();
        StompSession s2 = connect();
        try {
            String dest = "/topic/case/created";
            Duo duo = subscribeTwo(s1, s2, dest, 1);

            // Hit the REST controller (PublishController) which uses the publisher behind the scenes
            String url = String.format("http://localhost:%d/api/events/create/CASE-2", port);
            rest.postForEntity(url, null, Void.class);

            boolean ok1 = duo.a.await();
            boolean ok2 = duo.b.await();
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

