package dev.nilswitt.mission_manager.data.services;

import dev.nilswitt.mission_manager.data.entities.LogBookEntry;
import dev.nilswitt.mission_manager.data.entities.Mission;
import dev.nilswitt.mission_manager.data.repositories.LogBookEntryRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class LogBookEntryService {

    private final LogBookEntryRepository logBookEntryRepository;

    public LogBookEntryService(LogBookEntryRepository logBookEntryRepository) {
        this.logBookEntryRepository = logBookEntryRepository;
    }

    public List<LogBookEntry> findByMission(Mission mission) {
        return logBookEntryRepository.findByMission(mission);
    }

    public Page<LogBookEntry> findByMission(Mission mission, Pageable pageable) {
        return logBookEntryRepository.findByMission(mission, pageable);
    }

    public Optional<LogBookEntry> findById(UUID id) {
        return logBookEntryRepository.findById(id);
    }

    public boolean existsByMissionAndAttachment(Mission mission, UUID attachmentId) {
        return logBookEntryRepository.existsByMissionAndAttachments_Id(mission, attachmentId);
    }

    public Optional<LogBookEntry> findBySubmissionId(String submissionId) {
        return logBookEntryRepository.findBySubmissionId(submissionId);
    }

    public LogBookEntry save(LogBookEntry logBookEntry) {
        return logBookEntryRepository.save(logBookEntry);
    }

    public void deleteById(UUID id) {
        logBookEntryRepository.deleteById(id);
    }
}
