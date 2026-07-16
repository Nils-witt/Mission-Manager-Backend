package dev.nilswitt.mission_manager.data.services;

import dev.nilswitt.mission_manager.data.entities.Mission;
import dev.nilswitt.mission_manager.data.entities.UserMissionAssignment;
import dev.nilswitt.mission_manager.data.repositories.UserMissionAssignmentRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class UserMissionAssignmentService {

    private final UserMissionAssignmentRepository userMissionAssignmentRepository;

    public UserMissionAssignmentService(UserMissionAssignmentRepository userMissionAssignmentRepository) {
        this.userMissionAssignmentRepository = userMissionAssignmentRepository;
    }

    public List<UserMissionAssignment> findByMission(Mission mission) {
        return userMissionAssignmentRepository.findByMission(mission);
    }

    public Page<UserMissionAssignment> findByMission(Mission mission, UUID userId, Pageable pageable) {
        if (userId == null) {
            return userMissionAssignmentRepository.findByMission(mission, pageable);
        }
        return userMissionAssignmentRepository.findByMissionAndUser_Id(mission, userId, pageable);
    }

    public Optional<UserMissionAssignment> findById(UUID id) {
        return userMissionAssignmentRepository.findById(id);
    }

    public UserMissionAssignment save(UserMissionAssignment userMissionAssignment) {
        return userMissionAssignmentRepository.save(userMissionAssignment);
    }

    public void deleteById(UUID id) {
        userMissionAssignmentRepository.deleteById(id);
    }
}
