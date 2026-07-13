package dev.nilswitt.mission_manager.data.dto;

import dev.nilswitt.mission_manager.data.entities.SecurityGroup;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;

@Getter
@Setter
public class AbstractEntityDto {

    private UUID id;
    private Instant createdAt;
    private Instant updatedAt;
    private String createdBy;
    private String modifiedBy;
    private Set<SecurityGroup.UserRoleScopeEnum> permissions;

    public AbstractEntityDto(UUID id, Instant createdAt, Instant updatedAt, String createdBy, String modifiedBy) {
        this.id = id;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.createdBy = createdBy;
        this.modifiedBy = modifiedBy;
    }
}
