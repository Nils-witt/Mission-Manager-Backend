package dev.nilswitt.mission_manager.data.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.Set;

@Getter
@Setter
@Entity
@Table(name = "mission")
public class Mission extends AbstractEntity {

    @NotBlank
    @Size(max = 100)
    @Column(nullable = false, length = 100)
    private String name;

    @ManyToOne(optional = false)
    @JoinColumn(name = "tenant_id", nullable = false)
    private Tenant tenant;

    @Column(name = "start_time")
    private LocalDateTime startTime;

    @Column(name = "end_time")
    private LocalDateTime endTime;

    @Embedded
    private EmbeddableLocation location = new EmbeddableLocation();

    @Size(max = 255)
    @Column(name = "street_address", length = 255)
    private String streetAddress;

    @OneToMany(mappedBy = "mission", orphanRemoval = true)
    private Set<UserMissionAssignment> userAssignments = new LinkedHashSet<>();

    @OneToMany(mappedBy = "mission", orphanRemoval = true)
    private Set<UserPosition> positions = new LinkedHashSet<>();

    @OneToMany(mappedBy = "mission", orphanRemoval = true)
    private Set<LogBookEntry> logBookEntries = new LinkedHashSet<>();

    public Double getLatitude() {
        return location.getLatitude();
    }

    public void setLatitude(Double latitude) {
        location.setLatitude(latitude);
    }

    public Double getLongitude() {
        return location.getLongitude();
    }

    public void setLongitude(Double longitude) {
        location.setLongitude(longitude);
    }
}