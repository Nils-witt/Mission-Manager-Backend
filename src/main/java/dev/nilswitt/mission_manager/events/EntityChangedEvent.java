package dev.nilswitt.mission_manager.events;

import dev.nilswitt.mission_manager.data.entities.AbstractEntity;

import java.util.UUID;

public record EntityChangedEvent<T extends AbstractEntity>(
    String className,
    T entity,
    ChangeType changeType,
    UUID id
) {}
