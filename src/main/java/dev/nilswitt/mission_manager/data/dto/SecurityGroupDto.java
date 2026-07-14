package dev.nilswitt.mission_manager.data.dto;

import dev.nilswitt.mission_manager.data.entities.SecurityGroup;
import dev.nilswitt.mission_manager.data.entities.SecurityRole;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@EqualsAndHashCode(callSuper = true)
@Data
public class SecurityGroupDto extends AbstractEntityDto {

    private String name;
    private String ssoGroupName;
    private List<SecurityRole> roles;

    public SecurityGroupDto(
        UUID id,
        Instant createdAt,
        Instant updatedAt,
        String createdBy,
        String modifiedBy,
        String name,
        List<SecurityRole> roles,
        String ssoGroupName
    ) {
        super(id, createdAt, updatedAt, createdBy, modifiedBy);
        this.name = name;
        this.roles = roles;
        this.ssoGroupName = ssoGroupName;
    }

    public SecurityGroupDto(SecurityGroup group) {
        super(group.getId(), group.getCreatedAt(), group.getUpdatedAt(), group.getCreatedBy(), group.getModifiedBy());
        this.name = group.getName();
        this.roles = group.getRoles().stream().toList();
        this.ssoGroupName = group.getSsoGroupName();
    }
}
