package dev.nilswitt.mission_manager.api.dto;

import dev.nilswitt.mission_manager.data.entities.SecurityGroup;
import dev.nilswitt.mission_manager.data.entities.SecurityRole;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

public record SecurityGroupResponse(
        UUID id,
        Instant createdAt,
        Instant updatedAt,
        String name,
        String ssoGroupName,
        Set<SecurityRole> roles,
        boolean builtIn,
        Set<SecurityGroup.UserRoleScopeEnum> permissions,
        UUID tenantId
) {
    public static SecurityGroupResponse from(SecurityGroup group, Set<SecurityGroup.UserRoleScopeEnum> permissions) {
        return new SecurityGroupResponse(
                group.getId(),
                group.getCreatedAt(),
                group.getUpdatedAt(),
                group.getName(),
                group.getSsoGroupName(),
                group.getRoles(),
                group.isBuiltIn(),
                permissions,
                group.getTenant().getId()
        );
    }
}
