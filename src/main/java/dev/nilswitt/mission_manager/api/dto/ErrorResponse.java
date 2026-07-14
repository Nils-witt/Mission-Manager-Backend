package dev.nilswitt.mission_manager.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

@Schema(description = "A human-readable error message")
public record ErrorResponse(String message) {}
