package dev.nilswitt.mission_manager.events;

import dev.nilswitt.mission_manager.data.entities.AbstractEntity;
import dev.nilswitt.mission_manager.data.entities.AuditLog;
import dev.nilswitt.mission_manager.data.services.AuditLogWriter;
import org.hibernate.event.spi.PostDeleteEvent;
import org.hibernate.event.spi.PostDeleteEventListener;
import org.hibernate.event.spi.PostInsertEvent;
import org.hibernate.event.spi.PostInsertEventListener;
import org.hibernate.event.spi.PostUpdateEvent;
import org.hibernate.event.spi.PostUpdateEventListener;
import org.hibernate.persister.entity.EntityPersister;
import org.hibernate.proxy.HibernateProxy;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.time.Instant;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

/**
 * Hooks into Hibernate's low-level flush events (rather than the JPA {@code @PostUpdate} callbacks used by
 * {@link EntityEventListener}) because only these give access to the pre-change property state needed to
 * record old vs. new values. Registered onto the session factory by {@link HibernateAuditListenerRegistrar}.
 *
 * <p>The resulting {@link AuditLog} row is written via {@link AuditLogWriter} deferred to
 * {@code afterCommit} of the enclosing transaction (or immediately if there is none) rather than
 * through the current Hibernate session: persisting mid-flush re-enters the in-progress action
 * queue and throws a {@code ConcurrentModificationException}.
 */
@Component
public class AuditEventListener implements PostInsertEventListener, PostUpdateEventListener, PostDeleteEventListener {

    private static final Set<String> EXCLUDED_FIELDS = Set.of("createdAt", "updatedAt", "createdBy", "modifiedBy");
    private static final Set<String> SENSITIVE_FIELDS = Set.of("password");

    private final ObjectMapper objectMapper;
    private final AuditLogWriter auditLogWriter;

    public AuditEventListener(ObjectMapper objectMapper, AuditLogWriter auditLogWriter) {
        this.objectMapper = objectMapper;
        this.auditLogWriter = auditLogWriter;
    }

    @Override
    public void onPostInsert(PostInsertEvent event) {
        record(event.getEntity(), event.getId(), null, event.getState(), event.getPersister(), ChangeType.CREATED);
    }

    @Override
    public void onPostUpdate(PostUpdateEvent event) {
        record(
            event.getEntity(),
            event.getId(),
            event.getOldState(),
            event.getState(),
            event.getPersister(),
            ChangeType.UPDATED
        );
    }

    @Override
    public void onPostDelete(PostDeleteEvent event) {
        record(event.getEntity(), event.getId(), event.getDeletedState(), null, event.getPersister(), ChangeType.DELETED);
    }

    private void record(
        Object entity,
        Object id,
        Object[] oldState,
        Object[] newState,
        EntityPersister persister,
        ChangeType changeType
    ) {
        if (!(entity instanceof AbstractEntity)) {
            return;
        }

        String[] propertyNames = persister.getPropertyNames();
        Map<String, Object> oldValues = new LinkedHashMap<>();
        Map<String, Object> newValues = new LinkedHashMap<>();

        for (int i = 0; i < propertyNames.length; i++) {
            String propertyName = propertyNames[i];
            if (EXCLUDED_FIELDS.contains(propertyName)) {
                continue;
            }

            Object oldValue = sanitize(oldState != null ? oldState[i] : null);
            Object newValue = sanitize(newState != null ? newState[i] : null);

            if (SENSITIVE_FIELDS.contains(propertyName)) {
                oldValue = oldValue == null ? null : "[REDACTED]";
                newValue = newValue == null ? null : "[REDACTED]";
            }

            if (changeType == ChangeType.UPDATED && Objects.equals(oldValue, newValue)) {
                continue;
            }

            if (oldState != null) {
                oldValues.put(propertyName, oldValue);
            }
            if (newState != null) {
                newValues.put(propertyName, newValue);
            }
        }

        if (changeType == ChangeType.UPDATED && oldValues.isEmpty() && newValues.isEmpty()) {
            return;
        }

        AuditLog auditLog = new AuditLog();
        auditLog.setEntityName(entity.getClass().getSimpleName());
        auditLog.setEntityId(toUuid(id));
        auditLog.setChangeType(changeType);
        auditLog.setChangedBy(currentUsername());
        auditLog.setChangedAt(Instant.now());
        auditLog.setOldValues(toJson(oldValues));
        auditLog.setNewValues(toJson(newValues));

        if (TransactionSynchronizationManager.isSynchronizationActive()) {
            TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
                @Override
                public void afterCommit() {
                    auditLogWriter.save(auditLog);
                }
            });
        } else {
            auditLogWriter.save(auditLog);
        }
    }

    private Object sanitize(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof HibernateProxy proxy) {
            return proxy.getHibernateLazyInitializer().getIdentifier();
        }
        if (value instanceof AbstractEntity abstractEntity) {
            return abstractEntity.getId();
        }
        if (value instanceof Enum<?> enumValue) {
            return enumValue.name();
        }
        if (value instanceof Collection<?> || value instanceof Map<?, ?>) {
            return null;
        }
        return value;
    }

    private UUID toUuid(Object id) {
        if (id instanceof UUID uuid) {
            return uuid;
        }
        return UUID.fromString(id.toString());
    }

    private String currentUsername() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated()) {
            return "system";
        }
        return authentication.getName();
    }

    private String toJson(Map<String, Object> values) {
        if (values.isEmpty()) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(values);
        } catch (JacksonException e) {
            return null;
        }
    }
}
