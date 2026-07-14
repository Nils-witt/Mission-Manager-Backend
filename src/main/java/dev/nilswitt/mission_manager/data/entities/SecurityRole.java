package dev.nilswitt.mission_manager.data.entities;

import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AllArgsConstructor;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
public class SecurityRole {

    @Enumerated(EnumType.STRING)
    private SecurityGroup.UserRoleTypeEnum type;

    @Enumerated(EnumType.STRING)
    private SecurityGroup.UserRoleScopeEnum scope;
}
