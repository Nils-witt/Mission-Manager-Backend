package dev.nilswitt.mission_manager.data.repositories;

import dev.nilswitt.mission_manager.data.entities.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {
    List<AuditLog> findByEntityNameAndEntityIdOrderByChangedAtDesc(String entityName, UUID entityId);

    List<AuditLog> findTop200ByOrderByChangedAtDesc();

    List<AuditLog> findTop200ByEntityNameOrderByChangedAtDesc(String entityName);

    List<AuditLog> findTop200ByEntityNameAndEntityIdOrderByChangedAtDesc(String entityName, UUID entityId);
}
