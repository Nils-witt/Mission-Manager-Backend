package dev.nilswitt.mission_manager.data.repositories;

import dev.nilswitt.mission_manager.data.entities.NotificationDestination;
import dev.nilswitt.mission_manager.data.entities.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface NotificationDestinationRepository
    extends JpaRepository<NotificationDestination, UUID>, JpaSpecificationExecutor<NotificationDestination> {
    List<NotificationDestination> findByUser(User user);
}
