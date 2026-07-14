package dev.nilswitt.mission_manager.data.entities;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
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

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "security_group_role", joinColumns = @JoinColumn(name = "group_id"))
    private Set<SecurityRole> roles = new HashSet<>();

    @Column(nullable = false)
    private boolean isBuiltIn = false;

    public SecurityGroup(String name) {
        this.name = name;
    }

    public SecurityGroup(String name, Set<SecurityRole> roles) {
        this.name = name;
        this.roles = roles;
    }

    public static List<SecurityRole> availableRoles() {
        ArrayList<SecurityRole> roles = new ArrayList<>();
        for (UserRoleTypeEnum type : UserRoleTypeEnum.values()) {
            for (UserRoleScopeEnum scope : UserRoleScopeEnum.values()) {
                roles.add(new SecurityRole(type, scope));
            }
        }
        return roles;
    }

    public List<SimpleGrantedAuthority> getGrantedAuthorities() {
        return this.roles.stream()
                .map(role -> new SimpleGrantedAuthority("ROLE_" + role.getType().name() + "_" + role.getScope().name()))
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
        POSITION,
        TENANT,
    }
}
