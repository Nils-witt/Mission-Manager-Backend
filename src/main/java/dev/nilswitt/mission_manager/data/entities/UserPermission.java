package dev.nilswitt.mission_manager.data.entities;

import dev.nilswitt.mission_manager.data.dto.AbstractEntityDto;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.Getter;
import lombok.Setter;

@Entity
public class UserPermission extends AbstractPermission {

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    @Getter
    @Setter
    private User user;

    public UserPermission() {}

    public AbstractEntityDto toDto() {
        return new AbstractEntityDto(
            this.getId(),
            this.getCreatedAt(),
            this.getUpdatedAt(),
            this.getCreatedBy(),
            this.getModifiedBy()
        );
    }
}
