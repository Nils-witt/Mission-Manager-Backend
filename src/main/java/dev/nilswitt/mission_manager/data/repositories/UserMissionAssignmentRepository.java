package dev.nilswitt.mission_manager.data.repositories;

import dev.nilswitt.mission_manager.data.entities.Mission;
import dev.nilswitt.mission_manager.data.entities.UserMissionAssignment;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface UserMissionAssignmentRepository extends JpaRepository<UserMissionAssignment, UUID> {
    List<UserMissionAssignment> findByMission(Mission mission);

    Page<UserMissionAssignment> findByMission(Mission mission, Pageable pageable);

    Page<UserMissionAssignment> findByMissionAndUser_Id(Mission mission, UUID userId, Pageable pageable);
}
