package dev.nilswitt.mission_manager.api.dto;

import java.util.Set;
import java.util.UUID;

public record UserPositionRequest(String name, Set<UUID> qualificationIds, UUID assignedUserId) {}
