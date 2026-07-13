package dev.nilswitt.mission_manager.events;

import jakarta.annotation.PostConstruct;
import jakarta.persistence.EntityManagerFactory;
import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.event.service.spi.EventListenerRegistry;
import org.hibernate.event.spi.EventType;
import org.springframework.stereotype.Component;

/**
 * Wires {@link AuditEventListener} into Hibernate's event pipeline. This can't be done via a
 * regular {@code @EntityListeners} annotation because those only expose the JPA {@code @Post*}
 * callbacks, which don't carry the pre-change property state.
 */
@Component
public class HibernateAuditListenerRegistrar {

    private final EntityManagerFactory entityManagerFactory;
    private final AuditEventListener auditEventListener;

    public HibernateAuditListenerRegistrar(EntityManagerFactory entityManagerFactory, AuditEventListener auditEventListener) {
        this.entityManagerFactory = entityManagerFactory;
        this.auditEventListener = auditEventListener;
    }

    @PostConstruct
    public void register() {
        SessionFactoryImplementor sessionFactory = entityManagerFactory.unwrap(SessionFactoryImplementor.class);
        EventListenerRegistry registry = sessionFactory.getServiceRegistry().getService(EventListenerRegistry.class);
        registry.appendListeners(EventType.POST_INSERT, auditEventListener);
        registry.appendListeners(EventType.POST_UPDATE, auditEventListener);
        registry.appendListeners(EventType.POST_DELETE, auditEventListener);
    }
}
