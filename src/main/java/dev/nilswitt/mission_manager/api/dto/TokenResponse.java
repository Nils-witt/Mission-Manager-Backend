package dev.nilswitt.mission_manager.api.dto;

import java.time.Instant;

public record TokenResponse(String token, Instant expiresAt) {}
