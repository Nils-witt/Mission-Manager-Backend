package dev.nilswitt.mission_manager.api.dto;

import dev.nilswitt.mission_manager.data.entities.QualificationType;
import dev.nilswitt.mission_manager.data.entities.SecurityGroup;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record QualificationTypeResponse(
    UUID id,
    Instant createdAt,
    Instant updatedAt,
    String name,
    Set<SecurityGroup.UserRoleScopeEnum> permissions
) {
    public static QualificationTypeResponse from(QualificationType type, Set<SecurityGroup.UserRoleScopeEnum> permissions) {
        return new QualificationTypeResponse(
            type.getId(),
            type.getCreatedAt(),
            type.getUpdatedAt(),
            type.getName(),
            permissions
        );
    }
}
