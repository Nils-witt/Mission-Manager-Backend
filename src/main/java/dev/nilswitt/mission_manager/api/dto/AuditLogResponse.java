package dev.nilswitt.mission_manager.api.dto;

import dev.nilswitt.mission_manager.data.entities.AuditLog;
import dev.nilswitt.mission_manager.data.entities.SecurityGroup;
import dev.nilswitt.mission_manager.events.ChangeType;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public record AuditLogResponse(
    UUID id,
    String entityName,
    UUID entityId,
    ChangeType changeType,
    String changedBy,
    Instant changedAt,
    List<FieldChange> changes,
    Set<SecurityGroup.UserRoleScopeEnum> permissions
) {
    public record FieldChange(String field, String oldValue, String newValue) {}

    public static AuditLogResponse from(AuditLog log, List<FieldChange> changes, Set<SecurityGroup.UserRoleScopeEnum> permissions) {
        return new AuditLogResponse(
            log.getId(),
            log.getEntityName(),
            log.getEntityId(),
            log.getChangeType(),
            log.getChangedBy(),
            log.getChangedAt(),
            changes,
            permissions
        );
    }
}
