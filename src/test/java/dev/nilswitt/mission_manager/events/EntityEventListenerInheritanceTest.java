package dev.nilswitt.mission_manager.events;

import static org.assertj.core.api.Assertions.assertThat;

import dev.nilswitt.mission_manager.data.entities.Tenant;
import dev.nilswitt.mission_manager.data.services.TenantService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.event.ApplicationEvents;
import org.springframework.test.context.event.RecordApplicationEvents;

/**
 * {@link EntityEventListener} is wired via {@code @EntityListeners} on {@link
 * dev.nilswitt.mission_manager.data.entities.AbstractEntity} rather than per-entity, so this
 * exercises a plain entity (Tenant) that never declared the listener itself, confirming JPA
 * listener inheritance actually applies it at runtime.
 */
@SpringBootTest
@RecordApplicationEvents
class EntityEventListenerInheritanceTest {

    @Autowired
    private TenantService tenantService;

    @Autowired
    private ApplicationEvents applicationEvents;

    @Test
    void persistingAPlainEntitySubclassPublishesAnEntityChangedEvent() {
        Tenant tenant = new Tenant("Inheritance Test Tenant " + System.nanoTime());
        Tenant saved = tenantService.save(tenant);

        assertThat(applicationEvents.stream(EntityChangedEvent.class))
            .anyMatch(event -> event.className().equals("Tenant") && event.id().equals(saved.getId()) && event.changeType() == ChangeType.CREATED);
    }
}
