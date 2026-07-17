package dev.nilswitt.mission_manager.data.services;

import dev.nilswitt.mission_manager.data.entities.EmbeddableLocation;
import dev.nilswitt.mission_manager.data.entities.StoredFile;
import dev.nilswitt.mission_manager.data.repositories.StoredFileRepository;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class StoredFileService {

    private final StoredFileRepository storedFileRepository;
    private final StoredFileStorageService storedFileStorageService;

    public StoredFileService(StoredFileRepository storedFileRepository, StoredFileStorageService storedFileStorageService) {
        this.storedFileRepository = storedFileRepository;
        this.storedFileStorageService = storedFileStorageService;
    }

    public Optional<StoredFile> findById(UUID id) {
        return storedFileRepository.findById(id);
    }

    public List<StoredFile> findAllById(Iterable<UUID> ids) {
        return storedFileRepository.findAllById(ids);
    }

    public StoredFile store(MultipartFile file, String name, EmbeddableLocation location) {
        String storedFilename = storedFileStorageService.store(file);

        StoredFile storedFile = new StoredFile();
        storedFile.setName(name != null && !name.isBlank() ? name : file.getOriginalFilename());
        storedFile.setFilepath(storedFilename);
        storedFile.setOriginalFileName(file.getOriginalFilename());
        if (location != null) {
            storedFile.setLocation(location);
        }

        return storedFileRepository.save(storedFile);
    }

    public Resource load(StoredFile storedFile) {
        return storedFileStorageService.load(storedFile.getFilepath());
    }

    public void deleteById(UUID id) {
        storedFileRepository
            .findById(id)
            .ifPresent(storedFile -> {
                storedFileStorageService.delete(storedFile.getFilepath());
                storedFileRepository.deleteById(id);
            });
    }
}
