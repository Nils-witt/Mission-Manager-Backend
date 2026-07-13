package dev.nilswitt.mission_manager.security.jwt;

import dev.nilswitt.mission_manager.data.entities.JWTTokenRegistration;
import dev.nilswitt.mission_manager.data.services.JWTTokenRegistrationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class JWTRegistry {

    private final ConcurrentHashMap<UUID, Boolean> validTokens = new ConcurrentHashMap<>();
    private final JWTTokenRegistrationService jwtTokenRegistrationService;

    public JWTRegistry(JWTTokenRegistrationService jwtTokenRegistrationService) {
        this.jwtTokenRegistrationService = jwtTokenRegistrationService;
    }

    public boolean isValid(UUID tokenId) {
        if (validTokens.get(tokenId) != null) {
            return validTokens.get(tokenId);
        }

        if (jwtTokenRegistrationService.findByTokenId(tokenId).isPresent()) {
            validTokens.put(tokenId, true);
            return true;
        }
        return false;
    }

    public void addToken(JWTTokenRegistration token) {
        this.jwtTokenRegistrationService.save(token);
        this.validTokens.put(token.getTokenId(), true);
    }

    public void revokeToken(UUID tokenId) {
        validTokens.remove(tokenId);
    }
}
