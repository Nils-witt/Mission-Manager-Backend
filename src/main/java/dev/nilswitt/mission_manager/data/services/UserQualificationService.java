package dev.nilswitt.mission_manager.data.services;

import dev.nilswitt.mission_manager.data.entities.User;
import dev.nilswitt.mission_manager.data.entities.UserQualification;
import dev.nilswitt.mission_manager.data.repositories.UserQualificationRepository;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class UserQualificationService {

    private final UserQualificationRepository userQualificationRepository;
    private final CertificateStorageService certificateStorageService;

    public UserQualificationService(
        UserQualificationRepository userQualificationRepository,
        CertificateStorageService certificateStorageService
    ) {
        this.userQualificationRepository = userQualificationRepository;
        this.certificateStorageService = certificateStorageService;
    }

    public List<UserQualification> findByUser(User user) {
        return userQualificationRepository.findByUser(user);
    }

    public Page<UserQualification> findAll(Specification<UserQualification> spec, Pageable pageable) {
        return userQualificationRepository.findAll(spec, pageable);
    }

    public Optional<UserQualification> findById(UUID id) {
        return userQualificationRepository.findById(id);
    }

    public UserQualification save(UserQualification userQualification, MultipartFile certificate) {
        if (certificate != null && !certificate.isEmpty()) {
            String storedFilename = certificateStorageService.store(certificate);
            userQualification.setCertificateStoredFilename(storedFilename);
            userQualification.setCertificateOriginalFilename(certificate.getOriginalFilename());
            userQualification.setCertificateContentType(certificate.getContentType());
        }
        return userQualificationRepository.save(userQualification);
    }

    public Resource loadCertificate(UserQualification userQualification) {
        return certificateStorageService.load(userQualification.getCertificateStoredFilename());
    }

    public void deleteById(UUID id) {
        userQualificationRepository
            .findById(id)
            .ifPresent(uq -> {
                certificateStorageService.delete(uq.getCertificateStoredFilename());
                userQualificationRepository.deleteById(id);
            });
    }
}
