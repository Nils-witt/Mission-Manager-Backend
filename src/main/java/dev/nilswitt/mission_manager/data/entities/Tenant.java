package dev.nilswitt.mission_manager.data.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.ManyToMany;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.LinkedHashSet;
import java.util.Set;

@Setter
@Getter
@Entity
@NoArgsConstructor
public class Tenant extends AbstractEntity {
    @Column(name = "name", nullable = false)
    private String name;

    @ManyToMany(mappedBy = "tenants")
    private Set<User> users = new LinkedHashSet<>();

    public Tenant(String name) {
        this.name = name;
    }


}
