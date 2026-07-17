package dev.nilswitt.mission_manager.api;

import dev.nilswitt.mission_manager.api.dto.ErrorResponse;
import dev.nilswitt.mission_manager.api.dto.NotificationDestinationRequest;
import dev.nilswitt.mission_manager.api.dto.NotificationDestinationResponse;
import dev.nilswitt.mission_manager.api.dto.PageResponse;
import dev.nilswitt.mission_manager.data.entities.NotificationDestination;
import dev.nilswitt.mission_manager.data.entities.SecurityGroup;
import dev.nilswitt.mission_manager.data.entities.User;
import dev.nilswitt.mission_manager.data.services.NotificationDestinationService;
import dev.nilswitt.mission_manager.data.services.PushNotificationService;
import dev.nilswitt.mission_manager.data.services.UserService;
import dev.nilswitt.mission_manager.security.PermissionVerifier;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

import static dev.nilswitt.mission_manager.data.entities.SecurityGroup.UserRoleScopeEnum.EDIT;
import static dev.nilswitt.mission_manager.data.entities.SecurityGroup.UserRoleScopeEnum.VIEW;

@RestController
@RequestMapping("/api/users/{userId}/notification-destinations")
@Tag(name = "User Notification Destinations", description = "Push notification destinations (devices) registered for a user")
public class NotificationDestinationApiController {

    private final UserService userService;
    private final NotificationDestinationService notificationDestinationService;
    private final PushNotificationService pushNotificationService;
    private final PermissionVerifier permissionVerifier;

    public NotificationDestinationApiController(
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

    @GetMapping
    public PageResponse<NotificationDestinationResponse> list(
        @AuthenticationPrincipal User currentUser,
        @PathVariable UUID userId,
        @RequestParam(required = false) NotificationDestination.DeviceTypeEnum deviceType,
        @PageableDefault(size = 20, sort = "createdAt") Pageable pageable
    ) {
        User targetUser = findUserOrThrow(userId);
        requireAccess(currentUser, VIEW, targetUser);

        Specification<NotificationDestination> spec = Specifications.allOf(
            (root, query, cb) -> cb.equal(root.get("user").get("id"), userId),
            deviceTypeEquals(deviceType)
        );

        return PageResponse.from(
            notificationDestinationService.findAll(spec, pageable),
            destination -> NotificationDestinationResponse.from(destination, permissionVerifier.getScopes(targetUser, currentUser))
        );
    }

    private static Specification<NotificationDestination> deviceTypeEquals(NotificationDestination.DeviceTypeEnum deviceType) {
        if (deviceType == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("deviceType"), deviceType);
    }

    @PostMapping
    public ResponseEntity<?> add(
        @AuthenticationPrincipal User currentUser,
        @PathVariable UUID userId,
        @RequestBody NotificationDestinationRequest request
    ) {
        User targetUser = findUserOrThrow(userId);
        requireAccess(currentUser, EDIT, targetUser);

        ResponseEntity<?> validationError = validate(request);
        if (validationError != null) {
            return validationError;
        }

        NotificationDestination destination = new NotificationDestination();
        destination.setUser(targetUser);
        applyDetails(destination, request);

        try {
            notificationDestinationService.save(destination);
        } catch (DataIntegrityViolationException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse("This token is already registered."));
        }

        return ResponseEntity.status(HttpStatus.CREATED)
            .body(NotificationDestinationResponse.from(destination, permissionVerifier.getScopes(targetUser, currentUser)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(
        @AuthenticationPrincipal User currentUser,
        @PathVariable UUID userId,
        @PathVariable UUID id,
        @RequestBody NotificationDestinationRequest request
    ) {
        User targetUser = findUserOrThrow(userId);
        requireAccess(currentUser, EDIT, targetUser);
        NotificationDestination destination = findDestinationOrThrow(id, targetUser);

        ResponseEntity<?> validationError = validate(request);
        if (validationError != null) {
            return validationError;
        }

        applyDetails(destination, request);

        try {
            notificationDestinationService.save(destination);
        } catch (DataIntegrityViolationException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse("This token is already registered."));
        }

        return ResponseEntity.ok(NotificationDestinationResponse.from(destination, permissionVerifier.getScopes(targetUser, currentUser)));
    }

    @PostMapping("/{id}/test")
    public ResponseEntity<Void> sendTest(
        @AuthenticationPrincipal User currentUser,
        @PathVariable UUID userId,
        @PathVariable UUID id
    ) {
        User targetUser = findUserOrThrow(userId);
        requireAccess(currentUser, EDIT, targetUser);
        NotificationDestination destination = findDestinationOrThrow(id, targetUser);

        pushNotificationService.sendToDestination(destination, "Test notification", "This is a test notification from Mission Manager.");

        return ResponseEntity.accepted().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
        @AuthenticationPrincipal User currentUser,
        @PathVariable UUID userId,
        @PathVariable UUID id
    ) {
        User targetUser = findUserOrThrow(userId);
        requireAccess(currentUser, EDIT, targetUser);
        findDestinationOrThrow(id, targetUser);

        notificationDestinationService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    private ResponseEntity<?> validate(NotificationDestinationRequest request) {
        if (request.deviceType() == null) {
            return ResponseEntity.badRequest().body(new ErrorResponse("Device type is required."));
        }
        if (request.token() == null || request.token().isBlank()) {
            return ResponseEntity.badRequest().body(new ErrorResponse("Token is required."));
        }
        if (request.name() == null || request.name().isBlank()) {
            return ResponseEntity.badRequest().body(new ErrorResponse("Name is required."));
        }
        return null;
    }

    private void applyDetails(NotificationDestination destination, NotificationDestinationRequest request) {
        destination.setDeviceType(request.deviceType());
        destination.setToken(request.token());
        destination.setName(request.name());
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

    private void requireAccess(User currentUser, SecurityGroup.UserRoleScopeEnum scope, User target) {
        if (!permissionVerifier.hasAccess(currentUser, scope, target)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
    }
}
