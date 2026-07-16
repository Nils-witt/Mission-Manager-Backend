package dev.nilswitt.mission_manager.api;

import dev.nilswitt.mission_manager.api.dto.AssignedUserRequest;
import dev.nilswitt.mission_manager.api.dto.ErrorResponse;
import dev.nilswitt.mission_manager.api.dto.PageResponse;
import dev.nilswitt.mission_manager.api.dto.UserPositionRequest;
import dev.nilswitt.mission_manager.api.dto.UserPositionResponse;
import dev.nilswitt.mission_manager.data.entities.Mission;
import dev.nilswitt.mission_manager.data.entities.User;
import dev.nilswitt.mission_manager.data.entities.UserPosition;
import dev.nilswitt.mission_manager.data.services.MissionService;
import dev.nilswitt.mission_manager.data.services.QualificationService;
import dev.nilswitt.mission_manager.data.services.UserPositionService;
import dev.nilswitt.mission_manager.data.services.UserService;
import dev.nilswitt.mission_manager.security.PermissionVerifier;
import io.swagger.v3.oas.annotations.tags.Tag;
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
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Set;
import java.util.UUID;

import static dev.nilswitt.mission_manager.data.entities.SecurityGroup.UserRoleScopeEnum.EDIT;

@RestController
@RequestMapping("/api/missions/{missionId}/positions")
@Tag(name = "Mission Positions", description = "Positions belonging to a mission; a position cannot exist without its mission and cannot be moved to another one")
public class MissionPositionApiController {

    private final MissionService missionService;
    private final UserPositionService userPositionService;
    private final QualificationService qualificationService;
    private final UserService userService;
    private final PermissionVerifier permissionVerifier;

    public MissionPositionApiController(
        MissionService missionService,
        UserPositionService userPositionService,
        QualificationService qualificationService,
        UserService userService,
        PermissionVerifier permissionVerifier
    ) {
        this.missionService = missionService;
        this.userPositionService = userPositionService;
        this.qualificationService = qualificationService;
        this.userService = userService;
        this.permissionVerifier = permissionVerifier;
    }

    @GetMapping
    public PageResponse<UserPositionResponse> list(
        @AuthenticationPrincipal User currentUser,
        @PathVariable UUID missionId,
        @RequestParam(required = false) String name,
        @RequestParam(required = false) UUID assignedUserId,
        @PageableDefault(size = 20, sort = "createdAt") Pageable pageable
    ) {
        Mission mission = findMissionOrThrow(missionId);
        requireAccess(currentUser, mission);

        Specification<UserPosition> spec = Specifications.allOf(
            (root, query, cb) -> cb.equal(root.get("mission").get("id"), missionId),
            nameContains(name),
            assignedUserEquals(assignedUserId)
        );

        return PageResponse.from(
            userPositionService.findAll(spec, pageable),
            position -> UserPositionResponse.from(position, permissionVerifier.getScopes(mission, currentUser))
        );
    }

    private static Specification<UserPosition> nameContains(String name) {
        if (name == null || name.isBlank()) {
            return null;
        }
        return (root, query, cb) -> cb.like(cb.lower(root.get("name")), "%" + name.toLowerCase() + "%");
    }

    private static Specification<UserPosition> assignedUserEquals(UUID assignedUserId) {
        if (assignedUserId == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("assignedUser").get("id"), assignedUserId);
    }

    @PostMapping
    public ResponseEntity<?> add(
        @AuthenticationPrincipal User currentUser,
        @PathVariable UUID missionId,
        @RequestBody UserPositionRequest request
    ) {
        Mission mission = findMissionOrThrow(missionId);
        requireAccess(currentUser, mission);

        if (request.name() == null || request.name().isBlank()) {
            return ResponseEntity.badRequest().body(new ErrorResponse("Name is required."));
        }

        UserPosition position = new UserPosition();
        position.setMission(mission);
        position.setName(request.name());
        applyQualifications(position, request.qualificationIds());
        applyAssignedUser(position, request.assignedUserId());

        userPositionService.save(position);

        return ResponseEntity.status(HttpStatus.CREATED)
            .body(UserPositionResponse.from(position, permissionVerifier.getScopes(mission, currentUser)));
    }

    @PostMapping("/{id}/assigned-user")
    public UserPositionResponse updateAssignedUser(
        @AuthenticationPrincipal User currentUser,
        @PathVariable UUID missionId,
        @PathVariable UUID id,
        @RequestBody AssignedUserRequest request
    ) {
        Mission mission = findMissionOrThrow(missionId);
        requireAccess(currentUser, mission);
        UserPosition position = findPositionOrThrow(id, mission);

        applyAssignedUser(position, request.assignedUserId());
        userPositionService.save(position);

        return UserPositionResponse.from(position, permissionVerifier.getScopes(mission, currentUser));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
        @AuthenticationPrincipal User currentUser,
        @PathVariable UUID missionId,
        @PathVariable UUID id
    ) {
        Mission mission = findMissionOrThrow(missionId);
        requireAccess(currentUser, mission);
        findPositionOrThrow(id, mission);

        userPositionService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    private void applyQualifications(UserPosition position, Set<UUID> qualificationIds) {
        position.getQualifications().clear();
        if (qualificationIds != null) {
            qualificationIds.forEach(qId -> qualificationService.findById(qId).ifPresent(position.getQualifications()::add));
        }
    }

    private void applyAssignedUser(UserPosition position, UUID assignedUserId) {
        position.setAssignedUser(assignedUserId != null ? userService.findById(assignedUserId).orElse(null) : null);
    }

    private UserPosition findPositionOrThrow(UUID id, Mission mission) {
        UserPosition position = userPositionService
            .findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!position.getMission().getId().equals(mission.getId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return position;
    }

    private Mission findMissionOrThrow(UUID id) {
        return missionService.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    private void requireAccess(User currentUser, Mission mission) {
        if (!permissionVerifier.hasAccess(currentUser, EDIT, mission)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
    }
}
