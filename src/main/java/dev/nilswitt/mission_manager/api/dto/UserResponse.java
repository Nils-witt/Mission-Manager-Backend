package dev.nilswitt.mission_manager.api.dto;

import dev.nilswitt.mission_manager.data.entities.SecurityGroup;
import dev.nilswitt.mission_manager.data.entities.Tenant;
import dev.nilswitt.mission_manager.data.entities.User;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public record UserResponse(
    UUID id,
    Instant createdAt,
    Instant updatedAt,
    String username,
    String firstName,
    String lastName,
    String email,
    boolean enabled,
    boolean locked,
    UUID primaryTenantId,
    Set<UUID> tenantIds,
    Set<UUID> securityGroupIds,
    Set<SecurityGroup.UserRoleScopeEnum> permissions,
    Set<String> roles
) {
    public static UserResponse from(User user, Set<SecurityGroup.UserRoleScopeEnum> permissions) {
        return new UserResponse(
            user.getId(),
            user.getCreatedAt(),
            user.getUpdatedAt(),
            user.getUsername(),
            user.getFirstName(),
            user.getLastName(),
            user.getEmail(),
            user.isEnabled(),
            user.isLocked(),
            user.getPrimaryTenant() != null ? user.getPrimaryTenant().getId() : null,
            user.getTenants().stream().map(Tenant::getId).collect(Collectors.toSet()),
            user.getSecurityGroups().stream().map(SecurityGroup::getId).collect(Collectors.toSet()),
            permissions,
            user.getSecurityGroups().stream().flatMap(group -> group.getRoles().stream()).collect(Collectors.toSet())
        );
    }
}
