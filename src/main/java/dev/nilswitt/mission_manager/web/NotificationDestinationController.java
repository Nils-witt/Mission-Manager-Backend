package dev.nilswitt.mission_manager.web;

import dev.nilswitt.mission_manager.data.entities.NotificationDestination;
import dev.nilswitt.mission_manager.data.entities.User;
import dev.nilswitt.mission_manager.data.services.NotificationDestinationService;
import dev.nilswitt.mission_manager.data.services.PushNotificationService;
import dev.nilswitt.mission_manager.data.services.UserService;
import dev.nilswitt.mission_manager.security.PermissionVerifier;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

import static dev.nilswitt.mission_manager.data.entities.SecurityGroup.UserRoleScopeEnum.EDIT;

@Controller
@RequestMapping("/users/{userId}/notification-destinations")
public class NotificationDestinationController {

    private final UserService userService;
    private final NotificationDestinationService notificationDestinationService;
    private final PushNotificationService pushNotificationService;
    private final PermissionVerifier permissionVerifier;

    public NotificationDestinationController(
        UserService userService,
        NotificationDestinationService notificationDestinationService,
        PushNotificationService pushNotificationService,
        PermissionVerifier permissionVerifier
    ) {
        this.userService = userService;
        this.notificationDestinationService = notificationDestinationService;
        this.pushNotificationService = pushNotificationService;
        this.permissionVerifier = permissionVerifier;
    }

    @PostMapping
    public String add(
        @AuthenticationPrincipal User currentUser,
        @PathVariable UUID userId,
        @RequestParam(required = false) NotificationDestination.DeviceTypeEnum deviceType,
        @RequestParam(required = false) String token,
        @RequestParam(required = false) String name
    ) {
        User targetUser = findUserOrThrow(userId);
        requireAccess(currentUser, targetUser);

        if (deviceType == null) {
            return redirectWithError(userId, "Device type is required.");
        }
        if (token == null || token.isBlank()) {
            return redirectWithError(userId, "Token is required.");
        }
        if (name == null || name.isBlank()) {
            return redirectWithError(userId, "Name is required.");
        }

        NotificationDestination destination = new NotificationDestination();
        destination.setUser(targetUser);
        destination.setDeviceType(deviceType);
        destination.setToken(token);
        destination.setName(name);

        try {
            notificationDestinationService.save(destination);
        } catch (DataIntegrityViolationException ex) {
            return redirectWithError(userId, "This token is already registered.");
        }

        return "redirect:/users/" + userId + "/edit";
    }

    @PostMapping("/{id}/test")
    public String sendTest(
        @AuthenticationPrincipal User currentUser,
        @PathVariable UUID userId,
        @PathVariable UUID id
    ) {
        User targetUser = findUserOrThrow(userId);
        requireAccess(currentUser, targetUser);
        NotificationDestination destination = findDestinationOrThrow(id, targetUser);

        pushNotificationService.sendToDestination(destination, "Test notification", "This is a test notification from Mission Manager.");

        return "redirect:/users/" + userId + "/edit?notificationMessage="
            + UriUtils.encode("Test notification sent to '" + destination.getName() + "'.", StandardCharsets.UTF_8);
    }

    @PostMapping("/{id}/delete")
    public String delete(
        @AuthenticationPrincipal User currentUser,
        @PathVariable UUID userId,
        @PathVariable UUID id
    ) {
        User targetUser = findUserOrThrow(userId);
        requireAccess(currentUser, targetUser);
        findDestinationOrThrow(id, targetUser);

        notificationDestinationService.deleteById(id);
        return "redirect:/users/" + userId + "/edit";
    }

    private NotificationDestination findDestinationOrThrow(UUID id, User targetUser) {
        NotificationDestination destination = notificationDestinationService
            .findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!destination.getUser().getId().equals(targetUser.getId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return destination;
    }

    private User findUserOrThrow(UUID id) {
        return userService.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    private void requireAccess(User currentUser, User target) {
        if (!permissionVerifier.hasAccess(currentUser, EDIT, target)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
    }

    private String redirectWithError(UUID userId, String message) {
        return "redirect:/users/" + userId + "/edit?notificationError=" + UriUtils.encode(message, StandardCharsets.UTF_8);
    }
}
