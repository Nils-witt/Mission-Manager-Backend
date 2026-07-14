package dev.nilswitt.mission_manager.api;

import dev.nilswitt.mission_manager.api.dto.ErrorResponse;
import dev.nilswitt.mission_manager.api.dto.TenantRequest;
import dev.nilswitt.mission_manager.api.dto.TenantResponse;
import dev.nilswitt.mission_manager.data.entities.SecurityGroup;
import dev.nilswitt.mission_manager.data.entities.Tenant;
import dev.nilswitt.mission_manager.data.entities.User;
import dev.nilswitt.mission_manager.data.services.TenantService;
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

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static dev.nilswitt.mission_manager.data.entities.SecurityGroup.UserRoleScopeEnum.CREATE;
import static dev.nilswitt.mission_manager.data.entities.SecurityGroup.UserRoleScopeEnum.DELETE;
import static dev.nilswitt.mission_manager.data.entities.SecurityGroup.UserRoleScopeEnum.EDIT;
import static dev.nilswitt.mission_manager.data.entities.SecurityGroup.UserRoleScopeEnum.VIEW;
import static dev.nilswitt.mission_manager.data.entities.SecurityGroup.UserRoleTypeEnum.SECURITYGROUP;

@RestController
@RequestMapping("/api/tenants")
@Tag(name = "Tenants", description = "CRUD operations for tenants")
public class TenantApiController {

    private final TenantService tenantService;
    private final PermissionVerifier permissionVerifier;

    public TenantApiController(TenantService tenantService, PermissionVerifier permissionVerifier) {
        this.tenantService = tenantService;
        this.permissionVerifier = permissionVerifier;
    }

    @GetMapping
    public List<TenantResponse> list(@AuthenticationPrincipal User currentUser) {
        requireScope(currentUser, VIEW);
        return tenantService.findAll().stream().map(tenant -> TenantResponse.from(tenant, permissions(currentUser))).toList();
    }

    @GetMapping("/{id}")
    public TenantResponse get(@AuthenticationPrincipal User currentUser, @PathVariable UUID id) {
        requireScope(currentUser, VIEW);
        return TenantResponse.from(findTenantOrThrow(id), permissions(currentUser));
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
        if (!PermissionVerifier.hasAnyScope(currentUser, SECURITYGROUP, scope)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
    }

    private Set<SecurityGroup.UserRoleScopeEnum> permissions(User currentUser) {
        return permissionVerifier.getScopes(SECURITYGROUP, currentUser);
    }
}
