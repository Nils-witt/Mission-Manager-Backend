package dev.nilswitt.mission_manager.data.services;

import dev.nilswitt.mission_manager.data.entities.Qualification;
import dev.nilswitt.mission_manager.data.repositories.QualificationRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class QualificationService {

    private final QualificationRepository qualificationRepository;

    public QualificationService(QualificationRepository qualificationRepository) {
        this.qualificationRepository = qualificationRepository;
    }

    public List<Qualification> findAll() {
        return qualificationRepository.findAll();
    }

    public Optional<Qualification> findById(UUID id) {
        return qualificationRepository.findById(id);
    }

    public Optional<Qualification> findByName(String name) {
        return qualificationRepository.findByName(name);
    }

    public Qualification save(Qualification qualification) {
        return qualificationRepository.save(qualification);
    }

    public void deleteById(UUID id) {
        qualificationRepository.deleteById(id);
    }
}
