package dev.nilswitt.mission_manager.api.dto;

import dev.nilswitt.mission_manager.data.entities.Qualification;
import dev.nilswitt.mission_manager.data.entities.SecurityGroup;
import dev.nilswitt.mission_manager.data.entities.UserPosition;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public record UserPositionResponse(
    UUID id,
    Instant createdAt,
    Instant updatedAt,
    UUID missionId,
    String name,
    Set<UUID> qualificationIds,
    UUID assignedUserId,
    String assignedUsername,
    Set<SecurityGroup.UserRoleScopeEnum> permissions
) {
    public static UserPositionResponse from(UserPosition position, Set<SecurityGroup.UserRoleScopeEnum> permissions) {
        return new UserPositionResponse(
            position.getId(),
            position.getCreatedAt(),
            position.getUpdatedAt(),
            position.getMission().getId(),
            position.getName(),
            position.getQualifications().stream().map(Qualification::getId).collect(Collectors.toSet()),
            position.getAssignedUser() != null ? position.getAssignedUser().getId() : null,
            position.getAssignedUser() != null ? position.getAssignedUser().getUsername() : null,
            permissions
        );
    }
}
