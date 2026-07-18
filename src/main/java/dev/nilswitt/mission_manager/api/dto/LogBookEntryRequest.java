package dev.nilswitt.mission_manager.api.dto;

import dev.nilswitt.mission_manager.data.entities.EmbeddableLocation;

import java.util.Set;
import java.util.UUID;

public record LogBookEntryRequest(String text, String sender, String recipient,
                                  Set<UUID> attachmentIds, String submissionId,
                                  EmbeddableLocation location) {
}
