package dev.nilswitt.mission_manager.api.dto;

import dev.nilswitt.mission_manager.data.entities.SecurityGroup;
import dev.nilswitt.mission_manager.data.entities.Tenant;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record TenantResponse(
    UUID id,
    Instant createdAt,
    Instant updatedAt,
    String name,
    Set<SecurityGroup.UserRoleScopeEnum> permissions
) {
    public static TenantResponse from(Tenant tenant, Set<SecurityGroup.UserRoleScopeEnum> permissions) {
        return new TenantResponse(
            tenant.getId(),
            tenant.getCreatedAt(),
            tenant.getUpdatedAt(),
            tenant.getName(),
            permissions
        );
    }
}
