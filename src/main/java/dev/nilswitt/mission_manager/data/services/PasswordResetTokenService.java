package dev.nilswitt.mission_manager.data.services;


import dev.nilswitt.mission_manager.data.entities.PasswordResetToken;
import dev.nilswitt.mission_manager.data.entities.User;
import dev.nilswitt.mission_manager.data.repositories.PasswordResetTokenRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class PasswordResetTokenService {

    private final PasswordResetTokenRepository passwordResetTokenRepository;

    public PasswordResetTokenService(PasswordResetTokenRepository passwordResetTokenRepository) {
        this.passwordResetTokenRepository = passwordResetTokenRepository;
    }

    public List<PasswordResetToken> findAll() {
        return passwordResetTokenRepository.findAll();
    }

    public Optional<PasswordResetToken> findById(UUID id) {
        return passwordResetTokenRepository.findById(id);
    }

    public PasswordResetToken save(PasswordResetToken token) {
        return passwordResetTokenRepository.save(token);
    }

    public void deleteById(UUID id) {
        passwordResetTokenRepository.deleteById(id);
    }

    public Optional<PasswordResetToken> findByTokenHash(String tokenHash) {
        return passwordResetTokenRepository.findByTokenHash(tokenHash);
    }

    public void deleteByUser(User user) {
        passwordResetTokenRepository.deleteByUser(user);
    }
}
