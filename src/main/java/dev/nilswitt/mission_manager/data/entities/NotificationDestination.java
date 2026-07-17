package dev.nilswitt.mission_manager.data.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(name = "notification_destination")
public class NotificationDestination extends AbstractEntity {

    @Enumerated(EnumType.STRING)
    @Column(name = "device_type", nullable = false, length = 20)
    private DeviceTypeEnum deviceType;

    @NotBlank
    @Column(name = "token", nullable = false, unique = true, length = 4096)
    private String token;

    @NotBlank
    @Size(max = 100)
    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    public enum DeviceTypeEnum {
        ANDROID,
        IOS,
        WEB,
    }
}
