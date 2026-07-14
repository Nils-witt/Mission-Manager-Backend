package dev.nilswitt.mission_manager.api.dto;

import dev.nilswitt.mission_manager.data.entities.SecurityGroup;
import dev.nilswitt.mission_manager.data.entities.UserMissionAssignment;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

public record UserMissionAssignmentResponse(
    UUID id,
    Instant createdAt,
    Instant updatedAt,
    UUID missionId,
    UUID userId,
    String username,
    LocalDateTime startTime,
    LocalDateTime endTime,
    Set<SecurityGroup.UserRoleScopeEnum> permissions
) {
    public static UserMissionAssignmentResponse from(
        UserMissionAssignment assignment,
        Set<SecurityGroup.UserRoleScopeEnum> permissions
    ) {
        return new UserMissionAssignmentResponse(
            assignment.getId(),
            assignment.getCreatedAt(),
            assignment.getUpdatedAt(),
            assignment.getMission().getId(),
            assignment.getUser().getId(),
            assignment.getUser().getUsername(),
            assignment.getStartTime(),
            assignment.getEndTime(),
            permissions
        );
    }
}
