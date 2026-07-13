package dev.nilswitt.mission_manager.web;

import lombok.Data;

import java.util.HashSet;
import java.util.Set;

@Data
public class SecurityGroupFormModel {

    private String name;
    private String ssoGroupName;
    private Set<String> roles = new HashSet<>();
}
