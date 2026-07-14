package dev.nilswitt.mission_manager.api.dto;

import java.util.Set;
import java.util.UUID;

/**
 * Used for both create and update. On create, password is required; on update, a null/blank
 * password leaves the existing one unchanged (same semantics as the web form).
 */
public record UserRequest(
    String username,
    String firstName,
    String lastName,
    String email,
    String password,
    boolean enabled,
    boolean locked,
    UUID primaryTenantId,
    Set<UUID> securityGroupIds
) {}
