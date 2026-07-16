package dev.nilswitt.mission_manager.data.services;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

/**
 * Stores uploaded files on the local filesystem under a server-generated name, keyed off the
 * caller-supplied original filename only for its extension.
 */
@Slf4j
@Service
public class StoredFileStorageService {

    private final Path root;

    public StoredFileStorageService(@Value("${application.storage.stored-files-path}") String path) {
        this.root = Path.of(path).toAbsolutePath().normalize();
        try {
            Files.createDirectories(root);
        } catch (IOException e) {
            throw new UncheckedIOException("Unable to create stored file storage directory: " + root, e);
        }
    }

    public String store(MultipartFile file) {
        String storedFilename = UUID.randomUUID() + extensionOf(file.getOriginalFilename());
        Path target = resolve(storedFilename);
        try {
            file.transferTo(target);
        } catch (IOException e) {
            throw new UncheckedIOException("Unable to store file", e);
        }
        return storedFilename;
    }

    public Resource load(String storedFilename) {
        return new FileSystemResource(resolve(storedFilename));
    }

    public void delete(String storedFilename) {
        if (storedFilename == null) {
            return;
        }
        try {
            Files.deleteIfExists(resolve(storedFilename));
        } catch (IOException e) {
            log.warn("Failed to delete stored file '{}'", storedFilename, e);
        }
    }

    private Path resolve(String storedFilename) {
        Path resolved = root.resolve(storedFilename).normalize();
        if (!resolved.startsWith(root)) {
            throw new SecurityException("Invalid stored filename: " + storedFilename);
        }
        return resolved;
    }

    private String extensionOf(String originalFilename) {
        if (originalFilename == null) {
            return "";
        }
        int dot = originalFilename.lastIndexOf('.');
        return dot >= 0 ? originalFilename.substring(dot) : "";
    }
}
