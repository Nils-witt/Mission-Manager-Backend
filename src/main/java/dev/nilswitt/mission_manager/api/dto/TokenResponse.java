package dev.nilswitt.mission_manager.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.time.Instant;

@Schema(description = "An issued JWT and its expiration time")
public record TokenResponse(
    @Schema(description = "Bearer JWT to send as 'Authorization: Bearer <token>'") String token,
    @Schema(description = "Instant at which the token stops being valid") Instant expiresAt
) {}
