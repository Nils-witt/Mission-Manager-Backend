package dev.nilswitt.mission_manager.api;

import dev.nilswitt.mission_manager.api.dto.ErrorResponse;
import dev.nilswitt.mission_manager.api.dto.MissionRequest;
import dev.nilswitt.mission_manager.api.dto.MissionResponse;
import dev.nilswitt.mission_manager.data.entities.Mission;
import dev.nilswitt.mission_manager.data.entities.SecurityGroup;
import dev.nilswitt.mission_manager.data.entities.User;
import dev.nilswitt.mission_manager.data.services.MissionService;
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
    public List<MissionResponse> list(@AuthenticationPrincipal User currentUser) {
        requireScope(currentUser, VIEW);
        return missionService.findAll().stream()
            .map(mission -> MissionResponse.from(mission, permissionVerifier.getScopes(mission, currentUser)))
            .toList();
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
