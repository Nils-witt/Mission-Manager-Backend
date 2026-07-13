package dev.nilswitt.mission_manager.data.repositories;

import dev.nilswitt.mission_manager.data.entities.Mission;
import dev.nilswitt.mission_manager.data.entities.SecurityGroup;
import dev.nilswitt.mission_manager.data.entities.SecurityGroupPermission;
import dev.nilswitt.mission_manager.data.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SecurityGroupPermissionsRepository extends JpaRepository<SecurityGroupPermission, UUID> {

    List<SecurityGroupPermission> findByEntityUser(User entityUser);

    void deleteBySecurityGroup(SecurityGroup securityGroup);

    List<SecurityGroupPermission> findBySecurityGroupAndEntityUserNotNull(SecurityGroup securityGroup);

    List<SecurityGroupPermission> findBySecurityGroupAndMissionNotNull(SecurityGroup securityGroup);

    Optional<SecurityGroupPermission> findBySecurityGroupAndEntityUser(SecurityGroup securityGroup, User entityUser);

    Optional<SecurityGroupPermission> findBySecurityGroupAndMission(
            SecurityGroup securityGroup,
            Mission mission
    );
}
