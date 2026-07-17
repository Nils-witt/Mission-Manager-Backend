package dev.nilswitt.mission_manager.api.dto;

import dev.nilswitt.mission_manager.data.entities.NotificationDestination;
import dev.nilswitt.mission_manager.data.entities.SecurityGroup;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record NotificationDestinationResponse(
    UUID id,
    Instant createdAt,
    Instant updatedAt,
    UUID userId,
    NotificationDestination.DeviceTypeEnum deviceType,
    String token,
    String name,
    Set<SecurityGroup.UserRoleScopeEnum> permissions
) {
    public static NotificationDestinationResponse from(
        NotificationDestination destination,
        Set<SecurityGroup.UserRoleScopeEnum> permissions
    ) {
        return new NotificationDestinationResponse(
            destination.getId(),
            destination.getCreatedAt(),
            destination.getUpdatedAt(),
            destination.getUser().getId(),
            destination.getDeviceType(),
            destination.getToken(),
            destination.getName(),
            permissions
        );
    }
}
