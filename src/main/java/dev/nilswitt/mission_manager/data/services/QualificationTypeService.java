package dev.nilswitt.mission_manager.data.services;

import dev.nilswitt.mission_manager.data.entities.QualificationType;
import dev.nilswitt.mission_manager.data.repositories.QualificationTypeRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class QualificationTypeService {

    private final QualificationTypeRepository qualificationTypeRepository;

    public QualificationTypeService(QualificationTypeRepository qualificationTypeRepository) {
        this.qualificationTypeRepository = qualificationTypeRepository;
    }

    public List<QualificationType> findAll() {
        return qualificationTypeRepository.findAll();
    }

    public Page<QualificationType> findAll(String name, Pageable pageable) {
        if (name == null || name.isBlank()) {
            return qualificationTypeRepository.findAll(pageable);
        }
        return qualificationTypeRepository.findByNameContainingIgnoreCase(name, pageable);
    }

    public Optional<QualificationType> findById(UUID id) {
        return qualificationTypeRepository.findById(id);
    }

    public Optional<QualificationType> findByName(String name) {
        return qualificationTypeRepository.findByName(name);
    }

    public QualificationType save(QualificationType qualificationType) {
        return qualificationTypeRepository.save(qualificationType);
    }

    public void deleteById(UUID id) {
        qualificationTypeRepository.deleteById(id);
    }
}
