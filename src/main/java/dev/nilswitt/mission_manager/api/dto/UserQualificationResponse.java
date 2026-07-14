package dev.nilswitt.mission_manager.api.dto;

import dev.nilswitt.mission_manager.data.entities.SecurityGroup;
import dev.nilswitt.mission_manager.data.entities.UserQualification;

import java.time.Instant;
import java.time.LocalDate;
import java.util.Set;
import java.util.UUID;

public record UserQualificationResponse(
    UUID id,
    Instant createdAt,
    Instant updatedAt,
    UUID userId,
    UUID qualificationId,
    String qualificationName,
    LocalDate since,
    LocalDate expiry,
    boolean active,
    boolean expired,
    boolean hasCertificate,
    Set<SecurityGroup.UserRoleScopeEnum> permissions
) {
    public static UserQualificationResponse from(UserQualification uq, Set<SecurityGroup.UserRoleScopeEnum> permissions) {
        return new UserQualificationResponse(
            uq.getId(),
            uq.getCreatedAt(),
            uq.getUpdatedAt(),
            uq.getUser().getId(),
            uq.getQualification().getId(),
            uq.getQualification().getName(),
            uq.getSince(),
            uq.getExpiry(),
            uq.isActive(),
            uq.isExpired(),
            uq.hasCertificate(),
            permissions
        );
    }
}
