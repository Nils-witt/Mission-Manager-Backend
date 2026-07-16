package dev.nilswitt.mission_manager.api.dto;

import dev.nilswitt.mission_manager.data.entities.StoredFile;

import java.time.Instant;
import java.util.UUID;

public record StoredFileResponse(UUID id, Instant createdAt, String name, String originalFileName) {
    public static StoredFileResponse from(StoredFile storedFile) {
        return new StoredFileResponse(
            storedFile.getId(),
            storedFile.getCreatedAt(),
            storedFile.getName(),
            storedFile.getOriginalFileName()
        );
    }
}
