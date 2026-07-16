package dev.nilswitt.mission_manager.data.repositories;

import dev.nilswitt.mission_manager.data.entities.LogBookEntry;
import dev.nilswitt.mission_manager.data.entities.Mission;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface LogBookEntryRepository extends JpaRepository<LogBookEntry, UUID> {
    List<LogBookEntry> findByMission(Mission mission);

    Page<LogBookEntry> findByMission(Mission mission, Pageable pageable);

    boolean existsByMissionAndAttachments_Id(Mission mission, UUID attachmentId);
}
