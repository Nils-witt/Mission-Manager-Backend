package dev.nilswitt.mission_manager.api.dto;

import java.util.Set;

public record SecurityGroupRequest(String name, String ssoGroupName, Set<String> roles) {}
