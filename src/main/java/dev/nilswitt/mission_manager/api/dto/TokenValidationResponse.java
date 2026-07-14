package dev.nilswitt.mission_manager.api.dto;

import java.util.UUID;

public record TokenValidationResponse(boolean valid, UUID userId, String username) {}
