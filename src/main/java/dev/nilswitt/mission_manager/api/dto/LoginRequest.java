package dev.nilswitt.mission_manager.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "Credentials used to obtain a JWT")
public record LoginRequest(
    @Schema(description = "Account username", example = "admin") String username,
    @Schema(description = "Account password", example = "admin") String password
) {}
