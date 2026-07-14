package dev.nilswitt.mission_manager.api;

import dev.nilswitt.mission_manager.api.dto.LoginRequest;
import dev.nilswitt.mission_manager.api.dto.TokenResponse;
import dev.nilswitt.mission_manager.api.dto.TokenValidationResponse;
import dev.nilswitt.mission_manager.data.entities.User;
import dev.nilswitt.mission_manager.data.services.UserService;
import dev.nilswitt.mission_manager.security.jwt.JWTTokenComponent;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.UUID;

@RestController
@RequestMapping("/api/token")
public class TokenApiController {

    private final UserService userService;
    private final PasswordEncoder passwordEncoder;
    private final JWTTokenComponent jwtTokenComponent;

    public TokenApiController(UserService userService, PasswordEncoder passwordEncoder, JWTTokenComponent jwtTokenComponent) {
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.jwtTokenComponent = jwtTokenComponent;
    }

    @PostMapping
    public ResponseEntity<TokenResponse> login(@RequestBody LoginRequest request) {
        if (request.username() == null || request.password() == null) {
            return ResponseEntity.badRequest().build();
        }

        User user = userService.findByUsername(request.username()).orElse(null);
        if (user == null || !passwordEncoder.matches(request.password(), user.getPassword())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        if (!user.isEnabled() || !user.isAccountNonLocked()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }

        String token = jwtTokenComponent.generateToken(user, UUID.randomUUID());
        Instant expiresAt = Instant.now().plusMillis(jwtTokenComponent.getExpirationMs());

        return ResponseEntity.ok(new TokenResponse(token, expiresAt));
    }

    @GetMapping("/validate")
    public ResponseEntity<TokenValidationResponse> validate(@AuthenticationPrincipal User currentUser) {
        if (currentUser == null) {
            return ResponseEntity.ok(new TokenValidationResponse(false, null, null));
        }
        return ResponseEntity.ok(new TokenValidationResponse(true, currentUser.getId(), currentUser.getUsername()));
    }
}
