package dev.nilswitt.mission_manager.websocket;

import dev.nilswitt.mission_manager.events.EntityChangedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.time.Instant;

/**
 * Relays {@link EntityChangedEvent}s onto {@code /topic/entities/{EntityName}} so that any
 * connected STOMP client is notified in real time. Broadcasting is deferred to {@code
 * afterCommit} (mirroring {@link dev.nilswitt.mission_manager.events.AuditEventListener}) because
 * {@link dev.nilswitt.mission_manager.events.EntityEventListener} publishes on JPA {@code
 * @Post*} callbacks, which fire mid-flush, before the enclosing transaction is guaranteed to
 * commit.
 */
@Component
public class EntityChangeBroadcaster {

    private final SimpMessagingTemplate messagingTemplate;

    public EntityChangeBroadcaster(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    @EventListener
    public void onEntityChanged(EntityChangedEvent<?> event) {
        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    broadcast(event);
                }
            });
        } else {
            broadcast(event);
        }
    }

    private void broadcast(EntityChangedEvent<?> event) {
        EntityUpdateMessage message = new EntityUpdateMessage(event.className(), event.id(), event.changeType(), Instant.now());
        messagingTemplate.convertAndSend("/topic/entities/" + event.className(), message);
    }
}
