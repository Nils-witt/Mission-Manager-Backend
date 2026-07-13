package dev.nilswitt.mission_manager.data.services;

import dev.nilswitt.mission_manager.data.entities.Mission;
import dev.nilswitt.mission_manager.data.entities.User;
import dev.nilswitt.mission_manager.data.entities.UserPermission;
import dev.nilswitt.mission_manager.data.repositories.UserPermissionsRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class UserPermissionService {

    private final UserPermissionsRepository userPermissionsRepository;

    public UserPermissionService(UserPermissionsRepository userPermissionsRepository) {
        this.userPermissionsRepository = userPermissionsRepository;
    }

    public List<UserPermission> findAll() {
        return userPermissionsRepository.findAll();
    }

    public Optional<UserPermission> findById(UUID id) {
        return userPermissionsRepository.findById(id);
    }

    public UserPermission save(UserPermission userPermission) {
        return userPermissionsRepository.save(userPermission);
    }

    public void deleteById(UUID id) {
        userPermissionsRepository.deleteById(id);
    }

    public void delete(UserPermission userPermission) {
        userPermissionsRepository.delete(userPermission);
    }


    public Optional<UserPermission> findByUserAndMission(User user, Mission mission) {
        return userPermissionsRepository.findByUserAndMission(user, mission);
    }

    public List<UserPermission> findByUserAndMissionNotNull(User user) {
        return userPermissionsRepository.findByUserAndMissionNotNull(user).stream()
                .toList();
    }

    public Optional<UserPermission> findByUserAndEntityUser(User user, User target) {
        return userPermissionsRepository.findByUserAndEntityUser(user, target);
    }

    public List<UserPermission> findByUserAndEntityUserNotNull(User user) {
        return userPermissionsRepository.findByUserAndEntityUserNotNull(user);
    }
}
