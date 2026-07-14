package dev.nilswitt.mission_manager.api.dto;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.UUID;

@Schema(description = "An email to dispatch to a user, everyone in a tenant, or everyone in a security group")
public record EmailRequest(
    @Schema(description = "USER, TENANT, or GROUP", example = "USER") String recipientType,
    @Schema(description = "Id of the user, tenant, or security group to notify") UUID recipientId,
    String subject,
    String body
) {}
