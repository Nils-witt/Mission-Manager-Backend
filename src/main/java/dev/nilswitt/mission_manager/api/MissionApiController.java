package dev.nilswitt.mission_manager.api;

import dev.nilswitt.mission_manager.api.dto.ErrorResponse;
import dev.nilswitt.mission_manager.api.dto.MissionRequest;
import dev.nilswitt.mission_manager.api.dto.MissionResponse;
import dev.nilswitt.mission_manager.api.dto.PageResponse;
import dev.nilswitt.mission_manager.data.entities.Mission;
import dev.nilswitt.mission_manager.data.entities.SecurityGroup;
import dev.nilswitt.mission_manager.data.entities.User;
import dev.nilswitt.mission_manager.data.services.MissionService;
import dev.nilswitt.mission_manager.data.services.TenantService;
import dev.nilswitt.mission_manager.security.PermissionVerifier;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static dev.nilswitt.mission_manager.data.entities.SecurityGroup.UserRoleScopeEnum.CREATE;
import static dev.nilswitt.mission_manager.data.entities.SecurityGroup.UserRoleScopeEnum.DELETE;
import static dev.nilswitt.mission_manager.data.entities.SecurityGroup.UserRoleScopeEnum.EDIT;
import static dev.nilswitt.mission_manager.data.entities.SecurityGroup.UserRoleScopeEnum.VIEW;
import static dev.nilswitt.mission_manager.data.entities.SecurityGroup.UserRoleTypeEnum.MISSION;

@RestController
@RequestMapping("/api/missions")
@Tag(name = "Missions", description = "CRUD operations for missions")
public class MissionApiController {

    private final MissionService missionService;
    private final TenantService tenantService;
    private final PermissionVerifier permissionVerifier;

    public MissionApiController(MissionService missionService, TenantService tenantService, PermissionVerifier permissionVerifier) {
        this.missionService = missionService;
        this.tenantService = tenantService;
        this.permissionVerifier = permissionVerifier;
    }

    @GetMapping
    public PageResponse<MissionResponse> list(
        @AuthenticationPrincipal User currentUser,
        @RequestParam(required = false) String name,
        @RequestParam(required = false) UUID tenantId,
        @RequestParam(required = false) LocalDateTime startAfter,
        @RequestParam(required = false) LocalDateTime startBefore,
        @PageableDefault(size = 20, sort = "createdAt") Pageable pageable
    ) {
        Specification<Mission> spec = Specifications.allOf(
            nameContains(name),
            tenantEquals(tenantId),
            startAfter(startAfter),
            startBefore(startBefore)
        );

        Page<Mission> page = missionService.findAll(spec, pageable);
        List<MissionResponse> content = page.getContent().stream()
            .map(mission -> MissionResponse.from(mission, permissionVerifier.getScopes(mission, currentUser)))
            .filter(response -> response.permissions().contains(VIEW))
            .toList();

        return new PageResponse<>(content, page.getNumber(), page.getSize(), page.getTotalElements(), page.getTotalPages());
    }

    private static Specification<Mission> nameContains(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        return (root, query, cb) -> cb.like(cb.lower(root.get("name")), "%" + name.toLowerCase() + "%");
    }

    private static Specification<Mission> tenantEquals(UUID tenantId) {
        if (tenantId == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("tenant").get("id"), tenantId);
    }

    private static Specification<Mission> startAfter(LocalDateTime startAfter) {
        if (startAfter == null) {
            return null;
        }
        return (root, query, cb) -> cb.greaterThanOrEqualTo(root.get("startTime"), startAfter);
    }

    private static Specification<Mission> startBefore(LocalDateTime startBefore) {
        if (startBefore == null) {
            return null;
        }
        return (root, query, cb) -> cb.lessThanOrEqualTo(root.get("startTime"), startBefore);
    }

    @GetMapping("/{id}")
    public MissionResponse get(@AuthenticationPrincipal User currentUser, @PathVariable UUID id) {
        Mission target = findMissionOrThrow(id);
        requireAccess(currentUser, VIEW, target);
        return MissionResponse.from(target, permissionVerifier.getScopes(target, currentUser));
    }

    @PostMapping
    public ResponseEntity<?> create(@AuthenticationPrincipal User currentUser, @RequestBody MissionRequest request) {
        requireScope(currentUser, CREATE);

        if (request.tenantId() == null) {
            return ResponseEntity.badRequest().body(new ErrorResponse("Tenant is required."));
        }
        if (isEndBeforeStart(request)) {
            return ResponseEntity.badRequest().body(new ErrorResponse("End time cannot be before start time."));
        }

        Mission mission = new Mission();
        mission.setName(request.name());
        mission.setTenant(tenantService.findById(request.tenantId()).orElse(null));
        applyDetails(mission, request);

        try {
            missionService.save(mission);
        } catch (DataIntegrityViolationException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse("Unable to save this mission."));
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(MissionResponse.from(mission, permissionVerifier.getScopes(mission, currentUser)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(
        @AuthenticationPrincipal User currentUser,
        @PathVariable UUID id,
        @RequestBody MissionRequest request
    ) {
        Mission target = findMissionOrThrow(id);
        requireAccess(currentUser, EDIT, target);

        if (request.tenantId() == null) {
            return ResponseEntity.badRequest().body(new ErrorResponse("Tenant is required."));
        }
        if (isEndBeforeStart(request)) {
            return ResponseEntity.badRequest().body(new ErrorResponse("End time cannot be before start time."));
        }

        target.setName(request.name());
        target.setTenant(tenantService.findById(request.tenantId()).orElse(null));
        applyDetails(target, request);

        try {
            missionService.save(target);
        } catch (DataIntegrityViolationException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(new ErrorResponse("Unable to save this mission."));
        }

        return ResponseEntity.ok(MissionResponse.from(target, permissionVerifier.getScopes(target, currentUser)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@AuthenticationPrincipal User currentUser, @PathVariable UUID id) {
        Mission target = findMissionOrThrow(id);
        requireAccess(currentUser, DELETE, target);

        try {
            missionService.deleteById(id);
        } catch (DataIntegrityViolationException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse("Cannot delete a mission that still has permissions referencing it."));
        }

        return ResponseEntity.noContent().build();
    }

    private Mission findMissionOrThrow(UUID id) {
        return missionService.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    private void applyDetails(Mission mission, MissionRequest request) {
        mission.setStartTime(request.startTime());
        mission.setEndTime(request.endTime());
        mission.setLatitude(request.latitude());
        mission.setLongitude(request.longitude());
        mission.setStreetAddress(request.streetAddress());
    }

    private boolean isEndBeforeStart(MissionRequest request) {
        return request.startTime() != null && request.endTime() != null && request.endTime().isBefore(request.startTime());
    }

    private void requireScope(User currentUser, SecurityGroup.UserRoleScopeEnum scope) {
        if (!PermissionVerifier.hasAnyScope(currentUser, MISSION, scope)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
    }

    private void requireAccess(User currentUser, SecurityGroup.UserRoleScopeEnum scope, Mission target) {
        if (!permissionVerifier.hasAccess(currentUser, scope, target)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
    }
}
