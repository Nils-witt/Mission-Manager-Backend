package dev.nilswitt.mission_manager.websocket;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import dev.nilswitt.mission_manager.data.entities.LogBookEntry;
import dev.nilswitt.mission_manager.data.entities.Mission;
import dev.nilswitt.mission_manager.data.entities.Tenant;
import dev.nilswitt.mission_manager.data.entities.User;
import dev.nilswitt.mission_manager.data.services.MissionService;
import dev.nilswitt.mission_manager.data.services.TenantService;
import dev.nilswitt.mission_manager.data.services.UserService;
import dev.nilswitt.mission_manager.events.ChangeType;
import dev.nilswitt.mission_manager.events.EntityChangedEvent;
import dev.nilswitt.mission_manager.security.jwt.JWTTokenComponent;
import java.lang.reflect.Type;
import java.util.LinkedHashSet;
import java.util.UUID;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import org.jspecify.annotations.Nullable;
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
import org.springframework.web.socket.WebSocketHttpHeaders;
import org.springframework.web.socket.client.standard.StandardWebSocketClient;
import org.springframework.web.socket.messaging.WebSocketStompClient;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class EntityChangeBroadcasterTest {

    @LocalServerPort
    private int port;

    @Autowired
    private ApplicationEventPublisher applicationEventPublisher;

    @Autowired
    private TenantService tenantService;

    @Autowired
    private MissionService missionService;

    @Autowired
    private UserService userService;

    @Autowired
    private JWTTokenComponent jwtTokenComponent;

    @Test
    void entityChangedEventIsBroadcastToStompSubscribers() throws Exception {
        StompSession session = connect(null, new LinkedBlockingQueue<>());

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

    @Test
    void logBookEntryChangedEventIsBroadcastToAUserWithViewAccessToTheMission() throws Exception {
        Tenant tenant = tenantService.save(new Tenant("Broadcaster Test Tenant " + System.nanoTime()));
        Mission mission = new Mission();
        mission.setName("Broadcaster Test Mission " + System.nanoTime());
        mission.setTenant(tenant);
        mission = missionService.save(mission);

        User user = newUser("broadcaster-view-" + System.nanoTime(), tenant);
        String token = jwtTokenComponent.generateToken(user, UUID.randomUUID());

        StompSession session = connect(token, new LinkedBlockingQueue<>());

        BlockingQueue<EntityUpdateMessage> received = new LinkedBlockingQueue<>();
        UUID missionId = mission.getId();
        session.subscribe(
            "/topic/missions/" + missionId,
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

        LogBookEntry entry = new LogBookEntry();
        entry.setMission(mission);
        UUID entryId = UUID.randomUUID();

        // The STOMP SUBSCRIBE frame above is fire-and-forget, so re-publish until the broker has
        // finished registering the subscription rather than relying on a fixed sleep.
        EntityUpdateMessage message = null;
        long deadline = System.currentTimeMillis() + 5000;
        while (message == null && System.currentTimeMillis() < deadline) {
            applicationEventPublisher.publishEvent(new EntityChangedEvent<>("LogBookEntry", entry, ChangeType.CREATED, entryId));
            message = received.poll(300, TimeUnit.MILLISECONDS);
        }
        assertThat(message).isNotNull();
        assertThat(message.entityName()).isEqualTo("LogBookEntry");
        assertThat(message.id()).isEqualTo(entryId);
        assertThat(message.changeType()).isEqualTo(ChangeType.CREATED);

        session.disconnect();
    }

    @Test
    void subscribingToAMissionTopicWithoutAuthenticationIsRejected() throws Exception {
        Tenant tenant = tenantService.save(new Tenant("Broadcaster Reject Tenant " + System.nanoTime()));
        Mission mission = new Mission();
        mission.setName("Broadcaster Reject Mission " + System.nanoTime());
        mission.setTenant(tenant);
        mission = missionService.save(mission);

        BlockingQueue<Throwable> errors = new LinkedBlockingQueue<>();
        StompSession session = connect(null, errors);

        BlockingQueue<EntityUpdateMessage> received = new LinkedBlockingQueue<>();
        session.subscribe(
            "/topic/missions/" + mission.getId(),
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

        assertThat(errors.poll(5, TimeUnit.SECONDS)).isNotNull();

        LogBookEntry entry = new LogBookEntry();
        entry.setMission(mission);
        applicationEventPublisher.publishEvent(new EntityChangedEvent<>("LogBookEntry", entry, ChangeType.CREATED, UUID.randomUUID()));
        assertThat(received.poll(1, TimeUnit.SECONDS)).isNull();
    }

    @Test
    void subscribingToAMissionTopicWithoutViewAccessIsRejected() throws Exception {
        Tenant missionTenant = tenantService.save(new Tenant("Broadcaster Owner Tenant " + System.nanoTime()));
        Tenant otherTenant = tenantService.save(new Tenant("Broadcaster Outsider Tenant " + System.nanoTime()));
        Mission mission = new Mission();
        mission.setName("Broadcaster Private Mission " + System.nanoTime());
        mission.setTenant(missionTenant);
        mission = missionService.save(mission);

        User outsider = newUser("broadcaster-outsider-" + System.nanoTime(), otherTenant);
        String token = jwtTokenComponent.generateToken(outsider, UUID.randomUUID());

        BlockingQueue<Throwable> errors = new LinkedBlockingQueue<>();
        StompSession session = connect(token, errors);

        session.subscribe("/topic/missions/" + mission.getId(), new StompFrameHandler() {
            @Override
            public Type getPayloadType(StompHeaders headers) {
                return EntityUpdateMessage.class;
            }

            @Override
            public void handleFrame(StompHeaders headers, Object payload) {
                // no-op: this subscription is expected to be rejected before any frame arrives
            }
        });

        assertThat(errors.poll(5, TimeUnit.SECONDS)).isNotNull();
    }

    private User newUser(String username, Tenant tenant) {
        User user = new User(username, username + "@example.test", "Test", "User");
        user.setPrimaryTenant(tenant);
        user.setTenants(new LinkedHashSet<>(java.util.List.of(tenant)));
        return userService.save(user);
    }

    private StompSession connect(@Nullable String bearerToken, BlockingQueue<Throwable> errors) throws Exception {
        WebSocketStompClient stompClient = new WebSocketStompClient(new StandardWebSocketClient());
        MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();
        converter.setObjectMapper(new ObjectMapper().registerModule(new JavaTimeModule()));
        stompClient.setMessageConverter(converter);

        StompHeaders connectHeaders = new StompHeaders();
        if (bearerToken != null) {
            connectHeaders.add("Authorization", "Bearer " + bearerToken);
        }

        return stompClient
            .connectAsync(
                "ws://localhost:" + port + "/api/ws",
                new WebSocketHttpHeaders(),
                connectHeaders,
                new StompSessionHandlerAdapter() {
                    @Override
                    public void handleException(
                        StompSession session,
                        StompCommand command,
                        StompHeaders headers,
                        byte[] payload,
                        Throwable exception
                    ) {
                        errors.add(exception);
                    }

                    @Override
                    public void handleTransportError(StompSession session, Throwable exception) {
                        errors.add(exception);
                    }
                }
            )
            .get(5, TimeUnit.SECONDS);
    }
}
