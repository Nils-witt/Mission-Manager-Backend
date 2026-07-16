package dev.nilswitt.mission_manager.api;

import dev.nilswitt.mission_manager.api.dto.ErrorResponse;
import dev.nilswitt.mission_manager.api.dto.PageResponse;
import dev.nilswitt.mission_manager.api.dto.TenantRequest;
import dev.nilswitt.mission_manager.api.dto.TenantResponse;
import dev.nilswitt.mission_manager.data.entities.SecurityGroup;
import dev.nilswitt.mission_manager.data.entities.Tenant;
import dev.nilswitt.mission_manager.data.entities.User;
import dev.nilswitt.mission_manager.data.services.TenantService;
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
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static dev.nilswitt.mission_manager.data.entities.SecurityGroup.UserRoleScopeEnum.*;

@RestController
@RequestMapping("/api/tenants")
@Tag(name = "Tenants", description = "CRUD operations for tenants")
public class TenantApiController {

    private final TenantService tenantService;
    private final UserService userService;
    private final PermissionVerifier permissionVerifier;

    public TenantApiController(TenantService tenantService, UserService userService, PermissionVerifier permissionVerifier) {
        this.tenantService = tenantService;
        this.userService = userService;
        this.permissionVerifier = permissionVerifier;
    }

    @GetMapping
    public PageResponse<TenantResponse> list(
        @AuthenticationPrincipal User currentUser,
        @RequestParam(required = false) String name,
        @PageableDefault(size = 20, sort = "createdAt") Pageable pageable
    ) {
        Specification<Tenant> restriction = null;
        if (!hasViewScope(currentUser)) {
            Set<UUID> ownIds = ownTenants(currentUser).stream().map(Tenant::getId).collect(Collectors.toSet());
            restriction = idIn(ownIds);
        }
        Specification<Tenant> spec = Specifications.allOf(nameContains(name), restriction);
        return PageResponse.from(tenantService.findAll(spec, pageable), tenant -> TenantResponse.from(tenant, permissions(currentUser)));
    }

    private static Specification<Tenant> nameContains(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        return (root, query, cb) -> cb.like(cb.lower(root.get("name")), "%" + name.toLowerCase() + "%");
    }

    private static Specification<Tenant> idIn(Set<UUID> ids) {
        return (root, query, cb) -> root.get("id").in(ids);
    }

    @GetMapping("/{id}")
    public TenantResponse get(@AuthenticationPrincipal User currentUser, @PathVariable UUID id) {
        Tenant target = findTenantOrThrow(id);
        if (!hasViewScope(currentUser) && ownTenants(currentUser).stream().noneMatch(tenant -> tenant.getId().equals(id))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        return TenantResponse.from(target, permissions(currentUser));
    }

    @PostMapping
    public ResponseEntity<?> create(@AuthenticationPrincipal User currentUser, @RequestBody TenantRequest request) {
        requireScope(currentUser, CREATE);

        Tenant tenant = new Tenant(request.name());
        try {
            tenantService.save(tenant);
        } catch (DataIntegrityViolationException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse("A tenant with this name already exists."));
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(TenantResponse.from(tenant, permissions(currentUser)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(
            @AuthenticationPrincipal User currentUser,
            @PathVariable UUID id,
            @RequestBody TenantRequest request
    ) {
        requireScope(currentUser, EDIT);
        Tenant target = findTenantOrThrow(id);
        target.setName(request.name());

        try {
            tenantService.save(target);
        } catch (DataIntegrityViolationException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse("A tenant with this name already exists."));
        }

        return ResponseEntity.ok(TenantResponse.from(target, permissions(currentUser)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal User currentUser, @PathVariable UUID id) {
        requireScope(currentUser, DELETE);
        Tenant target = findTenantOrThrow(id);
        tenantService.deleteWithCleanup(target);
        return ResponseEntity.noContent().build();
    }

    private Tenant findTenantOrThrow(UUID id) {
        return tenantService.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    private void requireScope(User currentUser, SecurityGroup.UserRoleScopeEnum scope) {
        if (!PermissionVerifier.hasAnyScope(currentUser, SecurityGroup.UserRoleTypeEnum.TENANT, scope)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
    }

    private boolean hasViewScope(User currentUser) {
        return PermissionVerifier.hasAnyScope(currentUser, SecurityGroup.UserRoleTypeEnum.TENANT, VIEW);
    }

    private Set<Tenant> ownTenants(User currentUser) {
        return userService.findById(currentUser.getId()).map(User::getTenants).orElse(Set.of());
    }

    private Set<SecurityGroup.UserRoleScopeEnum> permissions(User currentUser) {
        return permissionVerifier.getScopes(SecurityGroup.UserRoleTypeEnum.TENANT, currentUser);
    }
}
