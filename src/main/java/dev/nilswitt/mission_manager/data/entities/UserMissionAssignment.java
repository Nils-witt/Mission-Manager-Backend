package dev.nilswitt.mission_manager.data.entities;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "user_mission_assignment")
public class UserMissionAssignment extends AbstractEntity {
    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(optional = false)
    @JoinColumn(name = "mission_id", nullable = false)
    private Mission mission;

    @Column
    private LocalDateTime startTime;
    
    @Column
    private LocalDateTime endTime;

}