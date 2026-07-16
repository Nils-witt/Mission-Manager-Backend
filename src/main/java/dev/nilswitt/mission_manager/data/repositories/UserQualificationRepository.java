package dev.nilswitt.mission_manager.data.repositories;

import dev.nilswitt.mission_manager.data.entities.User;
import dev.nilswitt.mission_manager.data.entities.UserQualification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface UserQualificationRepository extends JpaRepository<UserQualification, UUID>, JpaSpecificationExecutor<UserQualification> {
    List<UserQualification> findByUser(User user);
}
