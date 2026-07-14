package dev.nilswitt.mission_manager.api.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record UserMissionAssignmentRequest(UUID userId, LocalDateTime startTime, LocalDateTime endTime) {}
