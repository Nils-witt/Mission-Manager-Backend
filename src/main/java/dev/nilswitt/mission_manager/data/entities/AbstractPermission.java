package dev.nilswitt.mission_manager.data.entities;

import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MappedSuperclass;
import lombok.Getter;
import lombok.Setter;

/**
 * Base for permission assignment, established for which entities permissions can be assigned
 */
@MappedSuperclass
@Getter
@Setter
public abstract class AbstractPermission extends AbstractEntity {

    @Enumerated
    private SecurityGroup.UserRoleScopeEnum scope = SecurityGroup.UserRoleScopeEnum.VIEW;

    @ManyToOne
    @JoinColumn(name = "entity_user_id")
    private User entityUser;

    @ManyToOne
    @JoinColumn(name = "mission_id")
    private Mission mission;

    public AbstractPermission() {}
}
