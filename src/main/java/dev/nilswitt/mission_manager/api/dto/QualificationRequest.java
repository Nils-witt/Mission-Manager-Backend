package dev.nilswitt.mission_manager.api.dto;

import java.util.Set;
import java.util.UUID;

public record QualificationRequest(String name, UUID typeId, Set<UUID> includedQualificationIds) {}
