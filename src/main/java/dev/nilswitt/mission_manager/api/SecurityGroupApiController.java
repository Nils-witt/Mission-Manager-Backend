package dev.nilswitt.mission_manager.api;

import dev.nilswitt.mission_manager.api.dto.ErrorResponse;
import dev.nilswitt.mission_manager.api.dto.PageResponse;
import dev.nilswitt.mission_manager.api.dto.SecurityGroupRequest;
import dev.nilswitt.mission_manager.api.dto.SecurityGroupResponse;
import dev.nilswitt.mission_manager.data.entities.SecurityGroup;
import dev.nilswitt.mission_manager.data.entities.SecurityRole;
import dev.nilswitt.mission_manager.data.entities.Tenant;
import dev.nilswitt.mission_manager.data.entities.User;
import dev.nilswitt.mission_manager.data.services.SecurityGroupService;
import dev.nilswitt.mission_manager.data.services.TenantService;
import dev.nilswitt.mission_manager.security.PermissionVerifier;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Pageable;
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
@RequestMapping("/api/tenants/{tenantId}/security-groups")
@Tag(name = "Security Groups", description = "CRUD operations for security groups, scoped to a tenant")
public class SecurityGroupApiController {

    private final SecurityGroupService securityGroupService;
    private final TenantService tenantService;
    private final PermissionVerifier permissionVerifier;

    public SecurityGroupApiController(
        SecurityGroupService securityGroupService,
        TenantService tenantService,
        PermissionVerifier permissionVerifier
    ) {
        this.securityGroupService = securityGroupService;
        this.tenantService = tenantService;
        this.permissionVerifier = permissionVerifier;
    }

    @GetMapping
    public PageResponse<SecurityGroupResponse> list(
        @AuthenticationPrincipal User currentUser,
        @PathVariable UUID tenantId,
        @RequestParam(required = false) String name,
        @PageableDefault(size = 20, sort = "createdAt") Pageable pageable
    ) {
        requireScope(currentUser, VIEW);
        findTenantOrThrow(tenantId);
        return PageResponse.from(
            securityGroupService.findByTenantId(tenantId, name, pageable),
            group -> SecurityGroupResponse.from(group, permissions(currentUser))
        );
    }

    @GetMapping("/roles")
    public List<SecurityRole> listAvailableRoles(@AuthenticationPrincipal User currentUser, @PathVariable UUID tenantId) {
        requireScope(currentUser, VIEW);
        return SecurityGroup.availableRoles();
    }

    @GetMapping("/{id}")
    public SecurityGroupResponse get(
        @AuthenticationPrincipal User currentUser,
        @PathVariable UUID tenantId,
        @PathVariable UUID id
    ) {
        requireScope(currentUser, VIEW);
        return SecurityGroupResponse.from(findGroupOrThrow(tenantId, id), permissions(currentUser));
    }

    @PostMapping
    public ResponseEntity<?> create(
        @AuthenticationPrincipal User currentUser,
        @PathVariable UUID tenantId,
        @RequestBody SecurityGroupRequest request
    ) {
        requireScope(currentUser, CREATE);
        Tenant tenant = findTenantOrThrow(tenantId);

        SecurityGroup group = new SecurityGroup(request.name(), new HashSet<>(request.roles()));
        group.setSsoGroupName(request.ssoGroupName() == null ? "" : request.ssoGroupName());
        group.setTenant(tenant);

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
        @PathVariable UUID tenantId,
        @PathVariable UUID id,
        @RequestBody SecurityGroupRequest request
    ) {
        requireScope(currentUser, EDIT);
        SecurityGroup target = findGroupOrThrow(tenantId, id);

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
    public ResponseEntity<Void> delete(
        @AuthenticationPrincipal User currentUser,
        @PathVariable UUID tenantId,
        @PathVariable UUID id
    ) {
        requireScope(currentUser, DELETE);
        SecurityGroup target = findGroupOrThrow(tenantId, id);

        if (target.isBuiltIn()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Built-in security groups cannot be deleted.");
        }

        securityGroupService.deleteWithCleanup(target);
        return ResponseEntity.noContent().build();
    }

    private Tenant findTenantOrThrow(UUID tenantId) {
        return tenantService.findById(tenantId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    private SecurityGroup findGroupOrThrow(UUID tenantId, UUID id) {
        SecurityGroup group = securityGroupService.findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (group.getTenant() == null || !group.getTenant().getId().equals(tenantId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return group;
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
