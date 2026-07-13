package dev.nilswitt.mission_manager.data.services;

import dev.nilswitt.mission_manager.data.entities.AuditLog;
import dev.nilswitt.mission_manager.data.repositories.AuditLogRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * Persists audit rows in their own, brand-new transaction. Must not run inside the transaction
 * that triggered the change: writing during the same Hibernate flush that produced the
 * post-insert/update/delete event re-enters the in-progress action queue and blows up with a
 * {@code ConcurrentModificationException}.
 */
@Service
public class AuditLogWriter {

    private final AuditLogRepository auditLogRepository;

    public AuditLogWriter(AuditLogRepository auditLogRepository) {
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void save(AuditLog auditLog) {
        auditLogRepository.save(auditLog);
    }
}
