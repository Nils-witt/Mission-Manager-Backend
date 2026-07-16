package dev.nilswitt.mission_manager.data.repositories;

import dev.nilswitt.mission_manager.data.entities.Qualification;
import dev.nilswitt.mission_manager.data.entities.QualificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface QualificationRepository extends JpaRepository<Qualification, UUID> {
    Optional<Qualification> findByName(String name);

    List<Qualification> findByType(QualificationType type);

    Page<Qualification> findByNameContainingIgnoreCase(String name, Pageable pageable);
}
