package dev.nilswitt.mission_manager.api.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record MissionRequest(
    String name,
    UUID tenantId,
    LocalDateTime startTime,
    LocalDateTime endTime,
    Double latitude,
    Double longitude,
    String streetAddress
) {}
