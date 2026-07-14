package dev.nilswitt.mission_manager.api.dto;

import dev.nilswitt.mission_manager.data.entities.Qualification;
import dev.nilswitt.mission_manager.data.entities.SecurityGroup;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public record QualificationResponse(
    UUID id,
    Instant createdAt,
    Instant updatedAt,
    String name,
    UUID typeId,
    String typeName,
    Set<UUID> includedQualificationIds,
    Set<SecurityGroup.UserRoleScopeEnum> permissions
) {
    public static QualificationResponse from(Qualification qualification, Set<SecurityGroup.UserRoleScopeEnum> permissions) {
        return new QualificationResponse(
            qualification.getId(),
            qualification.getCreatedAt(),
            qualification.getUpdatedAt(),
            qualification.getName(),
            qualification.getType().getId(),
            qualification.getType().getName(),
            qualification.getIncludedQualifications().stream().map(Qualification::getId).collect(Collectors.toSet()),
            permissions
        );
    }
}
