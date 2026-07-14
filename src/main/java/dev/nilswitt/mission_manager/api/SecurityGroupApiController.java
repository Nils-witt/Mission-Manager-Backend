package dev.nilswitt.mission_manager.api;

import dev.nilswitt.mission_manager.api.dto.ErrorResponse;
import dev.nilswitt.mission_manager.api.dto.SecurityGroupRequest;
import dev.nilswitt.mission_manager.api.dto.SecurityGroupResponse;
import dev.nilswitt.mission_manager.data.entities.SecurityGroup;
import dev.nilswitt.mission_manager.data.entities.User;
import dev.nilswitt.mission_manager.data.services.SecurityGroupService;
import dev.nilswitt.mission_manager.security.PermissionVerifier;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.dao.DataIntegrityViolationException;
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
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static dev.nilswitt.mission_manager.data.entities.SecurityGroup.UserRoleScopeEnum.CREATE;
import static dev.nilswitt.mission_manager.data.entities.SecurityGroup.UserRoleScopeEnum.DELETE;
import static dev.nilswitt.mission_manager.data.entities.SecurityGroup.UserRoleScopeEnum.EDIT;
import static dev.nilswitt.mission_manager.data.entities.SecurityGroup.UserRoleScopeEnum.VIEW;
import static dev.nilswitt.mission_manager.data.entities.SecurityGroup.UserRoleTypeEnum.SECURITYGROUP;

@RestController
@RequestMapping("/api/security-groups")
@Tag(name = "Security Groups", description = "CRUD operations for security groups")
public class SecurityGroupApiController {

    private final SecurityGroupService securityGroupService;
    private final PermissionVerifier permissionVerifier;

    public SecurityGroupApiController(SecurityGroupService securityGroupService, PermissionVerifier permissionVerifier) {
        this.securityGroupService = securityGroupService;
        this.permissionVerifier = permissionVerifier;
    }

    @GetMapping
    public List<SecurityGroupResponse> list(@AuthenticationPrincipal User currentUser) {
        requireScope(currentUser, VIEW);
        return securityGroupService.findAll().stream().map(group -> SecurityGroupResponse.from(group, permissions(currentUser))).toList();
    }

    @GetMapping("/{id}")
    public SecurityGroupResponse get(@AuthenticationPrincipal User currentUser, @PathVariable UUID id) {
        requireScope(currentUser, VIEW);
        return SecurityGroupResponse.from(findGroupOrThrow(id), permissions(currentUser));
    }

    @PostMapping
    public ResponseEntity<?> create(@AuthenticationPrincipal User currentUser, @RequestBody SecurityGroupRequest request) {
        requireScope(currentUser, CREATE);

        SecurityGroup group = new SecurityGroup(request.name(), new HashSet<>(request.roles()));
        group.setSsoGroupName(request.ssoGroupName() == null ? "" : request.ssoGroupName());

        try {
            securityGroupService.save(group);
        } catch (DataIntegrityViolationException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse("A security group with this name already exists."));
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(SecurityGroupResponse.from(group, permissions(currentUser)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(
        @AuthenticationPrincipal User currentUser,
        @PathVariable UUID id,
        @RequestBody SecurityGroupRequest request
    ) {
        requireScope(currentUser, EDIT);
        SecurityGroup target = findGroupOrThrow(id);

        if (target.isBuiltIn()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Built-in security groups cannot be edited.");
        }

        target.setName(request.name());
        target.setSsoGroupName(request.ssoGroupName() == null ? "" : request.ssoGroupName());
        target.setRoles(new HashSet<>(request.roles()));

        try {
            securityGroupService.save(target);
        } catch (DataIntegrityViolationException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse("A security group with this name already exists."));
        }

        return ResponseEntity.ok(SecurityGroupResponse.from(target, permissions(currentUser)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal User currentUser, @PathVariable UUID id) {
        requireScope(currentUser, DELETE);
        SecurityGroup target = findGroupOrThrow(id);

        if (target.isBuiltIn()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Built-in security groups cannot be deleted.");
        }

        securityGroupService.deleteWithCleanup(target);
        return ResponseEntity.noContent().build();
    }

    private SecurityGroup findGroupOrThrow(UUID id) {
        return securityGroupService.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    private void requireScope(User currentUser, SecurityGroup.UserRoleScopeEnum scope) {
        if (!PermissionVerifier.hasAnyScope(currentUser, SECURITYGROUP, scope)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
    }

    private Set<SecurityGroup.UserRoleScopeEnum> permissions(User currentUser) {
        return permissionVerifier.getScopes(SECURITYGROUP, currentUser);
    }
}
