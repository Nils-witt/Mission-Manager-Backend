package dev.nilswitt.mission_manager.data.services;

import dev.nilswitt.mission_manager.data.entities.Mission;
import dev.nilswitt.mission_manager.data.entities.SecurityGroup;
import dev.nilswitt.mission_manager.data.entities.SecurityGroupPermission;
import dev.nilswitt.mission_manager.data.entities.User;
import dev.nilswitt.mission_manager.data.repositories.SecurityGroupPermissionsRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class SecurityGroupPermissionService {

    private final SecurityGroupPermissionsRepository securityGroupPermissionsRepository;

    public SecurityGroupPermissionService(SecurityGroupPermissionsRepository securityGroupPermissionsRepository) {
        this.securityGroupPermissionsRepository = securityGroupPermissionsRepository;
    }

    public List<SecurityGroupPermission> findAll() {
        return securityGroupPermissionsRepository.findAll();
    }

    public Optional<SecurityGroupPermission> findById(UUID id) {
        return securityGroupPermissionsRepository.findById(id);
    }

    public SecurityGroupPermission save(SecurityGroupPermission permission) {
        return securityGroupPermissionsRepository.save(permission);
    }

    public void deleteById(UUID id) {
        securityGroupPermissionsRepository.deleteById(id);
    }

    public void delete(SecurityGroupPermission permission) {
        securityGroupPermissionsRepository.delete(permission);
    }

    public void deleteBySecurityGroup(SecurityGroup securityGroup) {
        securityGroupPermissionsRepository.deleteBySecurityGroup(securityGroup);
    }

    public List<SecurityGroupPermission> findByEntityUser(User entityUser) {
        return securityGroupPermissionsRepository.findByEntityUser(entityUser);
    }


    public List<SecurityGroupPermission> findBySecurityGroupAndEntityUserNotNull(SecurityGroup securityGroup) {
        return securityGroupPermissionsRepository.findBySecurityGroupAndEntityUserNotNull(securityGroup);
    }

    public Optional<SecurityGroupPermission> findBySecurityGroupAndEntityUser(
            SecurityGroup securityGroup,
            User entityUser
    ) {
        return securityGroupPermissionsRepository.findBySecurityGroupAndEntityUser(securityGroup, entityUser);
    }


    public Optional<SecurityGroupPermission> findBySecurityGroupAndMission(
            SecurityGroup securityGroup,
            Mission mission
    ) {
        return securityGroupPermissionsRepository.findBySecurityGroupAndMission(securityGroup, mission);
    }

    public List<SecurityGroupPermission> findBySecurityGroupAndMissionNotNull(SecurityGroup sg) {
        return securityGroupPermissionsRepository.findBySecurityGroupAndMissionNotNull(sg)
                .stream()
                .toList();
    }
}
