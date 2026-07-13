package dev.nilswitt.mission_manager.data.repositories;

import dev.nilswitt.mission_manager.data.entities.JWTTokenRegistration;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JWTTokenRegistrationRepository extends JpaRepository<JWTTokenRegistration, UUID> {
    Optional<JWTTokenRegistration> findByTokenId(UUID tokenId);

    List<JWTTokenRegistration> findByUserId(UUID userId);

    void deleteByTokenId(UUID tokenId);
}
