package dev.nilswitt.mission_manager.data.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.JoinTable;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.LinkedHashSet;
import java.util.Set;

@Getter
@Setter
@Entity
public class UserPosition extends AbstractEntity {

    @NotBlank
    @Size(max = 100)
    @Column(nullable = false, length = 100)
    private String name;

    @ManyToOne(optional = false)
    @JoinColumn(name = "mission_id", nullable = false, updatable = false)
    private Mission mission;

    @ManyToMany
    @JoinTable(
        name = "user_position_qualifications",
        joinColumns = @JoinColumn(name = "user_position_id"),
        inverseJoinColumns = @JoinColumn(name = "qualification_id")
    )
    private Set<Qualification> qualifications = new LinkedHashSet<>();

    @ManyToOne
    @JoinColumn(name = "assigned_user_id")
    private User assignedUser;
}
