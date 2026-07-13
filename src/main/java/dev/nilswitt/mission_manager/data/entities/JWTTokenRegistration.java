package dev.nilswitt.mission_manager.data.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Setter
@Getter
@Entity
public class JWTTokenRegistration extends AbstractEntity {

    @Column(nullable = false, unique = true)
    private UUID tokenId;

    @Column(nullable = false)
    private UUID userId;

    @Column(nullable = false)
    private Instant expirationTime;

    public JWTTokenRegistration(UUID tokenId, UUID userId, Instant expirationTime) {
        this.tokenId = tokenId;
        this.userId = userId;
        this.expirationTime = expirationTime;
    }

    public JWTTokenRegistration() {}
}
