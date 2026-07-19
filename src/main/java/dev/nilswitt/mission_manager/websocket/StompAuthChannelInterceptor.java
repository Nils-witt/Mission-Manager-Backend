package dev.nilswitt.mission_manager.websocket;

import dev.nilswitt.mission_manager.data.entities.Mission;
import dev.nilswitt.mission_manager.data.entities.SecurityGroup;
import dev.nilswitt.mission_manager.data.entities.User;
import dev.nilswitt.mission_manager.data.services.MissionService;
import dev.nilswitt.mission_manager.security.PermissionVerifier;
import dev.nilswitt.mission_manager.security.jwt.JWTTokenComponent;
import org.springframework.lang.NonNull;
import org.springframework.lang.Nullable;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;

import java.security.Principal;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Authenticates STOMP {@code CONNECT} frames against the same JWTs used for the REST API (via
 * {@code Authorization: Bearer <token>} or a {@code token} STOMP header, mirroring {@link
 * dev.nilswitt.mission_manager.security.jwt.SpringSecurityJwtFilter}'s header/param fallback),
 * and authorizes {@code SUBSCRIBE} frames to {@code /topic/missions/{id}} against the
 * subscriber's VIEW permission for that mission.
 *
 * <p>The {@link Principal} set via {@code accessor.setUser(...)} while handling {@code CONNECT}
 * is remembered by Spring's STOMP support for the lifetime of the WebSocket session, so it is
 * available on every subsequent frame (including {@code SUBSCRIBE}) without needing to
 * re-authenticate.
 */
@Component
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private static final Pattern MISSION_TOPIC = Pattern.compile("^/topic/missions/([^/]+)$");

    private final JWTTokenComponent jwtTokenComponent;
    private final MissionService missionService;
    private final PermissionVerifier permissionVerifier;

    public StompAuthChannelInterceptor(
        JWTTokenComponent jwtTokenComponent,
        MissionService missionService,
        PermissionVerifier permissionVerifier
    ) {
        this.jwtTokenComponent = jwtTokenComponent;
        this.missionService = missionService;
        this.permissionVerifier = permissionVerifier;
    }

    @Override
    public Message<?> preSend(@NonNull Message<?> message, @NonNull MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);
        if (accessor == null || accessor.getCommand() == null) {
            return message;
        }

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            authenticate(accessor);
        } else if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            authorizeSubscription(accessor);
        }

        return message;
    }

    private void authenticate(StompHeaderAccessor accessor) {
        String token = extractToken(accessor);
        if (token == null || token.isBlank()) {
            return;
        }
        try {
            User user = jwtTokenComponent.getUserFromToken(token);
            if (user != null) {
                accessor.setUser(new UsernamePasswordAuthenticationToken(user, token, user.getAuthorities()));
            }
        } catch (Exception ex) {
            // Leave the session unauthenticated; subscriptions requiring an authenticated
            // user are rejected individually in authorizeSubscription().
        }
    }

    private void authorizeSubscription(StompHeaderAccessor accessor) {
        String destination = accessor.getDestination();
        if (destination == null) {
            return;
        }
        Matcher matcher = MISSION_TOPIC.matcher(destination);
        if (!matcher.matches()) {
            return;
        }

        UUID missionId;
        try {
            missionId = UUID.fromString(matcher.group(1));
        } catch (IllegalArgumentException e) {
            throw new AccessDeniedException("Invalid mission id in " + destination);
        }

        User user = resolveUser(accessor.getUser());
        if (user == null) {
            throw new AccessDeniedException("Authentication is required to subscribe to " + destination);
        }

        Mission mission = missionService.findById(missionId)
            .orElseThrow(() -> new AccessDeniedException("Mission not found: " + missionId));

        if (!permissionVerifier.hasAccess(user, SecurityGroup.UserRoleScopeEnum.VIEW, mission)) {
            throw new AccessDeniedException("Not permitted to subscribe to " + destination);
        }
    }

    @Nullable
    private String extractToken(StompHeaderAccessor accessor) {
        String authHeader = accessor.getFirstNativeHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            return authHeader.substring(7);
        }
        return accessor.getFirstNativeHeader("token");
    }

    @Nullable
    private User resolveUser(@Nullable Principal principal) {
        if (principal instanceof UsernamePasswordAuthenticationToken auth && auth.getPrincipal() instanceof User user) {
            return user;
        }
        return null;
    }
}
