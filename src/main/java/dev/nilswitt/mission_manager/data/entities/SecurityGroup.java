package dev.nilswitt.mission_manager.data.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import dev.nilswitt.mission_manager.events.EntityEventListener;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.Inheritance;
import jakarta.persistence.InheritanceType;
import jakarta.persistence.ManyToMany;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Entity
@Inheritance(strategy = InheritanceType.JOINED)
@EntityListeners(EntityEventListener.class)
@Getter
@Setter
@NoArgsConstructor
public class SecurityGroup extends AbstractEntity {

    @NotBlank
    @Size(max = 100)
    @Column(nullable = false, unique = true, length = 100)
    private String name;

    @Column(nullable = false, length = 255, columnDefinition = "")
    private String ssoGroupName = "";

    @ManyToMany(mappedBy = "securityGroups")
    @JsonIgnore
    private Set<User> users;

    @Column
    private Set<String> roles = new HashSet<>();

    @Column(nullable = false)
    private boolean isBuiltIn = false;

    public SecurityGroup(String name) {
        this.name = name;
    }

    public SecurityGroup(String name, Set<String> roles) {
        this.name = name;
        this.roles = roles;
    }

    public static List<String> availableRoles() {
        ArrayList<String> roles = new ArrayList<>();
        for (UserRoleTypeEnum type : UserRoleTypeEnum.values()) {
            for (UserRoleScopeEnum scope : UserRoleScopeEnum.values()) {
                String roleName = type.name() + "_" + scope.name();
                roles.add(roleName);
            }
        }
        return roles;
    }

    public List<SimpleGrantedAuthority> getGrantedAuthorities() {
        return this.roles.stream()
            .map(a -> new SimpleGrantedAuthority("ROLE_" + a))
            .toList();
    }

    public enum UserRoleScopeEnum {
        VIEW,
        EDIT,
        CREATE,
        DELETE,
        ADMIN,
    }

    public enum UserRoleTypeEnum {
        MAPOVERLAY,
        MAPBASELAYER,
        USER,
        MAPGROUP,
        SECURITYGROUP,
        UNIT,
        MAPITEM,
        GLOBAL,
        PHOTO,
        MISSIONGROUP,
        MISSION,
        PATIENT,
        UHS,
        AUDITLOG,
        EMAIL,
        QUALIFICATION,
    }
}
