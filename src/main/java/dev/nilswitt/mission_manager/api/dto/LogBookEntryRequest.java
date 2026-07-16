package dev.nilswitt.mission_manager.api.dto;

import java.util.Set;
import java.util.UUID;

public record LogBookEntryRequest(String text, String sender, String recipient,
                                  Set<UUID> attachmentIds) {
}
