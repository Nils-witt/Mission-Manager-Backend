package dev.nilswitt.mission_manager.data.repositories;

import dev.nilswitt.mission_manager.data.entities.QualificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.UUID;

@Repository
public interface QualificationTypeRepository extends JpaRepository<QualificationType, UUID> {
    Optional<QualificationType> findByName(String name);

    Page<QualificationType> findByNameContainingIgnoreCase(String name, Pageable pageable);
}
