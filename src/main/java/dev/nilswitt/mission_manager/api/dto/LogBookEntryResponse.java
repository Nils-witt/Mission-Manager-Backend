package dev.nilswitt.mission_manager.api.dto;

import dev.nilswitt.mission_manager.data.entities.EmbeddableLocation;
import dev.nilswitt.mission_manager.data.entities.LogBookEntry;
import dev.nilswitt.mission_manager.data.entities.SecurityGroup;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public record LogBookEntryResponse(
    UUID id,
    Instant createdAt,
    Instant updatedAt,
    UUID missionId,
    String text,
    String sender,
    String recipient,
    String author,
    String submissionId,
    EmbeddableLocation location,
    Set<StoredFileResponse> attachments,
    Set<SecurityGroup.UserRoleScopeEnum> permissions
) {
    public static LogBookEntryResponse from(LogBookEntry entry, Set<SecurityGroup.UserRoleScopeEnum> permissions) {
        return new LogBookEntryResponse(
            entry.getId(),
            entry.getCreatedAt(),
            entry.getUpdatedAt(),
            entry.getMission().getId(),
            entry.getText(),
            entry.getSender(),
            entry.getRecipient(),
            entry.getAuthor(),
            entry.getSubmissionId(),
            entry.getLocation(),
            entry.getAttachments().stream().map(StoredFileResponse::from).collect(Collectors.toSet()),
            permissions
        );
    }
}
