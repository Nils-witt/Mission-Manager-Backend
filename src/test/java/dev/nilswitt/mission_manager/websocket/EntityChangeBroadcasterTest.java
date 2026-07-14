package dev.nilswitt.mission_manager.websocket;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import dev.nilswitt.mission_manager.data.entities.Tenant;
import dev.nilswitt.mission_manager.events.ChangeType;
import dev.nilswitt.mission_manager.events.EntityChangedEvent;
import java.lang.reflect.Type;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.messaging.converter.MappingJackson2MessageConverter;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompFrameHandler;
import org.springframework.messaging.simp.stomp.StompHeaders;
import org.springframework.messaging.simp.stomp.StompSession;
import org.springframework.messaging.simp.stomp.StompSessionHandlerAdapter;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class EntityChangeBroadcasterTest {

    @LocalServerPort
    private int port;

    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;

    @Test
    void entityChangedEventIsBroadcastToStompSubscribers() throws Exception {
        WebSocketStompClient stompClient = new WebSocketStompClient(new StandardWebSocketClient());
        MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();
        converter.setObjectMapper(new ObjectMapper().registerModule(new JavaTimeModule()));
        stompClient.setMessageConverter(converter);

        StompSession session = stompClient
            .connectAsync(
                "ws://localhost:" + port + "/api/ws",
                new StompSessionHandlerAdapter() {
                    @Override
                    public void handleException(
                        StompSession session,
                        StompCommand command,
                        StompHeaders headers,
                        byte[] payload,
                        Throwable exception
                    ) {
                        exception.printStackTrace();
                    }

                    @Override
                    public void handleTransportError(StompSession session, Throwable exception) {
                        exception.printStackTrace();
                    }
                }
            )
            .get(5, TimeUnit.SECONDS);

        BlockingQueue<EntityUpdateMessage> received = new LinkedBlockingQueue<>();
        session.subscribe(
            "/topic/entities/Tenant",
            new StompFrameHandler() {
                @Override
                public Type getPayloadType(StompHeaders headers) {
                    return EntityUpdateMessage.class;
                }

                @Override
                public void handleFrame(StompHeaders headers, Object payload) {
                    received.add((EntityUpdateMessage) payload);
                }
            }
        );

        UUID entityId = UUID.randomUUID();
        Tenant tenant = new Tenant();

        // The STOMP SUBSCRIBE frame above is fire-and-forget, so re-publish until the broker has
        // finished registering the subscription rather than relying on a fixed sleep.
        EntityUpdateMessage message = null;
        long deadline = System.currentTimeMillis() + 5000;
        while (message == null && System.currentTimeMillis() < deadline) {
            applicationEventPublisher.publishEvent(new EntityChangedEvent<>("Tenant", tenant, ChangeType.CREATED, entityId));
            message = received.poll(300, TimeUnit.MILLISECONDS);
        }
        assertThat(message).isNotNull();
        assertThat(message.entityName()).isEqualTo("Tenant");
        assertThat(message.id()).isEqualTo(entityId);
        assertThat(message.changeType()).isEqualTo(ChangeType.CREATED);

        session.disconnect();
    }
}
