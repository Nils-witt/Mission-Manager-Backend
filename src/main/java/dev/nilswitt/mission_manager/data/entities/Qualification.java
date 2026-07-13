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
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.LinkedHashSet;
import java.util.Set;

@Getter
@Setter
@Entity
@NoArgsConstructor
public class Qualification extends AbstractEntity {

    @NotBlank
    @Size(max = 100)
    @Column(nullable = false, length = 100)
    private String name;

    @ManyToOne(optional = false)
    @JoinColumn(name = "qualification_type_id", nullable = false)
    private QualificationType type;

    @ManyToMany
    @JoinTable(
        name = "qualification_included_qualifications",
        joinColumns = @JoinColumn(name = "qualification_id"),
        inverseJoinColumns = @JoinColumn(name = "included_qualification_id")
    )
    private Set<Qualification> includedQualifications = new LinkedHashSet<>();

    public Qualification(String name, QualificationType type) {
        this.name = name;
        this.type = type;
    }
}
