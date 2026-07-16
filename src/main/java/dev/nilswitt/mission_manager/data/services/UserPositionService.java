package dev.nilswitt.mission_manager.data.services;

import dev.nilswitt.mission_manager.data.entities.Mission;
import dev.nilswitt.mission_manager.data.entities.UserPosition;
import dev.nilswitt.mission_manager.data.repositories.UserPositionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class UserPositionService {

    private final UserPositionRepository userPositionRepository;

    public UserPositionService(UserPositionRepository userPositionRepository) {
        this.userPositionRepository = userPositionRepository;
    }

    public List<UserPosition> findByMission(Mission mission) {
        return userPositionRepository.findByMission(mission);
    }

    public Page<UserPosition> findAll(Specification<UserPosition> spec, Pageable pageable) {
        return userPositionRepository.findAll(spec, pageable);
    }

    public Optional<UserPosition> findById(UUID id) {
        return userPositionRepository.findById(id);
    }

    public UserPosition save(UserPosition userPosition) {
        return userPositionRepository.save(userPosition);
    }

    public void deleteById(UUID id) {
        userPositionRepository.deleteById(id);
    }
}
