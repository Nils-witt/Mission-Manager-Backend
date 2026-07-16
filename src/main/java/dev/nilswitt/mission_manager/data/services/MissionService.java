package dev.nilswitt.mission_manager.data.services;

import dev.nilswitt.mission_manager.data.entities.Mission;
import dev.nilswitt.mission_manager.data.repositories.MissionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class MissionService {

    private final MissionRepository missionRepository;

    public MissionService(MissionRepository missionRepository) {
        this.missionRepository = missionRepository;
    }

    public List<Mission> findAll() {
        return missionRepository.findAll();
    }

    public Page<Mission> findAll(Specification<Mission> spec, Pageable pageable) {
        return missionRepository.findAll(spec, pageable);
    }

    public Optional<Mission> findById(UUID id) {
        return missionRepository.findById(id);
    }

    public Mission save(Mission mission) {
        return missionRepository.save(mission);
    }

    public void deleteById(UUID id) {
        missionRepository.deleteById(id);
    }
}
