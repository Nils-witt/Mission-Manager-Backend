package dev.nilswitt.mission_manager.api.dto;

import dev.nilswitt.mission_manager.data.entities.Mission;
import dev.nilswitt.mission_manager.data.entities.SecurityGroup;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

public record MissionResponse(
    UUID id,
    Instant createdAt,
    Instant updatedAt,
    String name,
    UUID tenantId,
    LocalDateTime startTime,
    LocalDateTime endTime,
    Double latitude,
    Double longitude,
    String streetAddress,
    Set<SecurityGroup.UserRoleScopeEnum> permissions
) {
    public static MissionResponse from(Mission mission, Set<SecurityGroup.UserRoleScopeEnum> permissions) {
        return new MissionResponse(
            mission.getId(),
            mission.getCreatedAt(),
            mission.getUpdatedAt(),
            mission.getName(),
            mission.getTenant().getId(),
            mission.getStartTime(),
            mission.getEndTime(),
            mission.getLatitude(),
            mission.getLongitude(),
            mission.getStreetAddress(),
            permissions
        );
    }
}
