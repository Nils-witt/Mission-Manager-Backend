package dev.nilswitt.mission_manager.api.dto;

import dev.nilswitt.mission_manager.data.entities.NotificationDestination;

public record NotificationDestinationRequest(NotificationDestination.DeviceTypeEnum deviceType, String token, String name) {}
