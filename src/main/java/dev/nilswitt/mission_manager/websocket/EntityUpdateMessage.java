package dev.nilswitt.mission_manager.websocket;

import dev.nilswitt.mission_manager.events.ChangeType;

import java.time.Instant;
import java.util.UUID;

public record EntityUpdateMessage(String entityName, UUID id, ChangeType changeType, Instant timestamp) {}
