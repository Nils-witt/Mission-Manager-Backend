package dev.nilswitt.mission_manager.data.repositories;

import dev.nilswitt.mission_manager.data.entities.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface UserPermissionsRepository extends JpaRepository<UserPermission, UUID> {

    List<UserPermission> findByEntityUser(User entityUser);

    List<UserPermission> findByUserAndMissionNotNull(User user);


    List<UserPermission> findByUserAndEntityUserNotNull(User user);

    Optional<UserPermission> findByUserAndEntityUser(User user, User entityUser);

    Optional<UserPermission> findByUserAndMission(User user, Mission mission);
}
