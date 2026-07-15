package dev.nilswitt.mission_manager.api.dto;

import dev.nilswitt.mission_manager.data.entities.SecurityRole;

import java.util.Set;
import java.util.UUID;

public record SecurityGroupRequest(String name, String ssoGroupName, Set<SecurityRole> roles, UUID tenantId) {
}
