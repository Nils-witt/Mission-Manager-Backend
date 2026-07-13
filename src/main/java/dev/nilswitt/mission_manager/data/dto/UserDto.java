package dev.nilswitt.mission_manager.data.dto;

import dev.nilswitt.mission_manager.data.entities.SecurityGroup;
import dev.nilswitt.mission_manager.data.entities.User;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@EqualsAndHashCode(callSuper = true)
@Data
public class UserDto extends AbstractEntityDto {

    private String username;
    private String email;
    private String firstName;
    private String lastName;

    private boolean enabled;
    private boolean locked;

    private Set<UUID> securityGroups;

    public UserDto(
            UUID id,
            Instant createdAt,
            Instant updatedAt,
            String createdBy,
            String modifiedBy,
            String username,
            String email,
            String firstName,
            String lastName,
            boolean enabled,
            boolean locked,
            Set<UUID> securityGroups
    ) {
        super(id, createdAt, updatedAt, createdBy, modifiedBy);
        this.username = username;
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.enabled = enabled;
        this.locked = locked;
        this.securityGroups = securityGroups;
    }

    public UserDto(User user) {
        super(user.getId(), user.getCreatedAt(), user.getUpdatedAt(), user.getCreatedBy(), user.getModifiedBy());
        this.username = user.getUsername();
        this.email = user.getEmail();
        this.firstName = user.getFirstName();
        this.lastName = user.getLastName();
        this.enabled = user.isEnabled();
        this.locked = user.isLocked();
        this.securityGroups = user.getSecurityGroups().stream().map(SecurityGroup::getId).collect(Collectors.toSet());
    }
}
