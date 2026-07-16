package dev.nilswitt.mission_manager.data.repositories;

import dev.nilswitt.mission_manager.data.entities.SecurityGroup;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SecurityGroupRepository extends JpaRepository<SecurityGroup, UUID> {
    Optional<SecurityGroup> findByName(String name);

    List<SecurityGroup> findBySsoGroupName(String ssoGroupName);

    List<SecurityGroup> findByTenant_Id(UUID tenantId);

    Page<SecurityGroup> findByTenant_Id(UUID tenantId, Pageable pageable);

    Page<SecurityGroup> findByTenant_IdAndNameContainingIgnoreCase(UUID tenantId, String name, Pageable pageable);


    @Modifying
    @Query(value = "DELETE FROM user_security_group WHERE group_id = :groupId", nativeQuery = true)
    void removeFromAllUsers(@Param("groupId") UUID groupId);

}
