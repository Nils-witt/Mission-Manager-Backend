package dev.nilswitt.mission_manager.api;

import dev.nilswitt.mission_manager.api.dto.ErrorResponse;
import dev.nilswitt.mission_manager.api.dto.UserRequest;
import dev.nilswitt.mission_manager.api.dto.UserResponse;
import dev.nilswitt.mission_manager.data.entities.SecurityGroup;
import dev.nilswitt.mission_manager.data.entities.Tenant;
import dev.nilswitt.mission_manager.data.entities.User;
import dev.nilswitt.mission_manager.data.services.SecurityGroupService;
import dev.nilswitt.mission_manager.data.services.TenantService;
import dev.nilswitt.mission_manager.data.services.UserService;
import dev.nilswitt.mission_manager.security.PermissionVerifier;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static dev.nilswitt.mission_manager.data.entities.SecurityGroup.UserRoleScopeEnum.CREATE;
import static dev.nilswitt.mission_manager.data.entities.SecurityGroup.UserRoleScopeEnum.DELETE;
import static dev.nilswitt.mission_manager.data.entities.SecurityGroup.UserRoleScopeEnum.EDIT;
import static dev.nilswitt.mission_manager.data.entities.SecurityGroup.UserRoleScopeEnum.VIEW;
import static dev.nilswitt.mission_manager.data.entities.SecurityGroup.UserRoleTypeEnum.USER;

@RestController
@RequestMapping("/api/users")
@Tag(name = "Users", description = "CRUD operations for users")
public class UserApiController {

    private final UserService userService;
    private final SecurityGroupService securityGroupService;
    private final TenantService tenantService;
    private final PermissionVerifier permissionVerifier;
    private final PasswordEncoder passwordEncoder;

    public UserApiController(
        UserService userService,
        SecurityGroupService securityGroupService,
        TenantService tenantService,
        PermissionVerifier permissionVerifier,
        PasswordEncoder passwordEncoder
    ) {
        this.userService = userService;
        this.securityGroupService = securityGroupService;
        this.tenantService = tenantService;
        this.permissionVerifier = permissionVerifier;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping
    public List<UserResponse> list(@AuthenticationPrincipal User currentUser) {
        requireScope(currentUser, VIEW);
        return userService.findAll().stream().map(target -> UserResponse.from(target, permissions(currentUser, target))).toList();
    }

    @GetMapping("/{id}")
    public UserResponse get(@AuthenticationPrincipal User currentUser, @PathVariable UUID id) {
        User target = findUserOrThrow(id);
        requireAccess(currentUser, VIEW, target);
        return UserResponse.from(target, permissions(currentUser, target));
    }

    @PostMapping
    public ResponseEntity<?> create(@AuthenticationPrincipal User currentUser, @RequestBody UserRequest request) {
        requireScope(currentUser, CREATE);

        if (request.password() == null || request.password().isBlank()) {
            return ResponseEntity.badRequest().body(new ErrorResponse("Password is required for new users."));
        }
        if (request.primaryTenantId() == null) {
            return ResponseEntity.badRequest().body(new ErrorResponse("Primary tenant is required."));
        }

        User user = new User(request.username(), request.email(), request.firstName(), request.lastName());
        user.setPassword(passwordEncoder.encode(request.password()));
        user.setEnabled(request.enabled());
        user.setLocked(request.locked());
        applySecurityGroups(user, request.securityGroupIds());
        applyPrimaryTenant(user, request.primaryTenantId());

        try {
            userService.save(user);
        } catch (DataIntegrityViolationException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse("A user with this username or email already exists."));
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(UserResponse.from(user, permissions(currentUser, user)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(
        @AuthenticationPrincipal User currentUser,
        @PathVariable UUID id,
        @RequestBody UserRequest request
    ) {
        User target = findUserOrThrow(id);
        requireAccess(currentUser, EDIT, target);

        if (request.primaryTenantId() == null) {
            return ResponseEntity.badRequest().body(new ErrorResponse("Primary tenant is required."));
        }

        target.setUsername(request.username());
        target.setEmail(request.email());
        target.setFirstName(request.firstName());
        target.setLastName(request.lastName());
        target.setEnabled(request.enabled());
        target.setLocked(request.locked());
        if (request.password() != null && !request.password().isBlank()) {
            target.setPassword(passwordEncoder.encode(request.password()));
        }
        applySecurityGroups(target, request.securityGroupIds());
        applyPrimaryTenant(target, request.primaryTenantId());

        try {
            userService.save(target);
        } catch (DataIntegrityViolationException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse("A user with this username or email already exists."));
        }

        return ResponseEntity.ok(UserResponse.from(target, permissions(currentUser, target)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal User currentUser, @PathVariable UUID id) {
        User target = findUserOrThrow(id);
        requireAccess(currentUser, DELETE, target);

        if (target.getId().equals(currentUser.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You cannot delete your own account.");
        }

        userService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    private void applySecurityGroups(User user, Set<UUID> groupIds) {
        user.getSecurityGroups().clear();
        if (groupIds != null) {
            groupIds.forEach(groupId -> securityGroupService.findById(groupId).ifPresent(user::addSecurityGroup));
        }
        securityGroupService.findByName("Everyone").ifPresent(user::addSecurityGroup);
    }

    private void applyPrimaryTenant(User user, UUID tenantId) {
        Tenant tenant = tenantService.findById(tenantId).orElse(null);
        user.setPrimaryTenant(tenant);
        if (tenant != null) {
            user.getTenants().add(tenant);
        }
    }

    private User findUserOrThrow(UUID id) {
        return userService.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    private void requireScope(User currentUser, SecurityGroup.UserRoleScopeEnum scope) {
        if (!PermissionVerifier.hasAnyScope(currentUser, USER, scope)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
    }

    private void requireAccess(User currentUser, SecurityGroup.UserRoleScopeEnum scope, User target) {
        if (!permissionVerifier.hasAccess(currentUser, scope, target)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
    }

    private Set<SecurityGroup.UserRoleScopeEnum> permissions(User currentUser, User target) {
        return permissionVerifier.getScopes(target, currentUser);
    }
}
