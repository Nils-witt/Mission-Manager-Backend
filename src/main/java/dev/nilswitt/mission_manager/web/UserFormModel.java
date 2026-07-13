package dev.nilswitt.mission_manager.web;

import lombok.Data;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Data
public class UserFormModel {

    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private String password;
    private boolean enabled = true;
    private boolean locked = false;
    private Set<UUID> securityGroupIds = new HashSet<>();
    private UUID primaryTenantId;
}
