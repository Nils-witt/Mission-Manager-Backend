package dev.nilswitt.mission_manager.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "Result of validating a JWT")
public record TokenValidationResponse(
    @Schema(description = "Whether the token is signed correctly, unexpired, and not revoked") boolean valid,
    @Schema(description = "ID of the user the token belongs to, or null if invalid") UUID userId,
    @Schema(description = "Username of the user the token belongs to, or null if invalid") String username
) {}
