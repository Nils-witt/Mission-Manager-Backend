package dev.nilswitt.mission_manager.api;

import dev.nilswitt.mission_manager.api.dto.ErrorResponse;
import dev.nilswitt.mission_manager.api.dto.PageResponse;
import dev.nilswitt.mission_manager.api.dto.UserMissionAssignmentRequest;
import dev.nilswitt.mission_manager.api.dto.UserMissionAssignmentResponse;
import dev.nilswitt.mission_manager.data.entities.Mission;
import dev.nilswitt.mission_manager.data.entities.User;
import dev.nilswitt.mission_manager.data.entities.UserMissionAssignment;
import dev.nilswitt.mission_manager.data.services.MissionService;
import dev.nilswitt.mission_manager.data.services.UserMissionAssignmentService;
import dev.nilswitt.mission_manager.data.services.UserService;
import dev.nilswitt.mission_manager.security.PermissionVerifier;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Pageable;
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

import java.util.UUID;

import static dev.nilswitt.mission_manager.data.entities.SecurityGroup.UserRoleScopeEnum.EDIT;

@RestController
@RequestMapping("/api/missions/{missionId}/users")
@Tag(name = "Mission User Assignments", description = "Users assigned to work a mission, with an optional time window")
public class MissionUserAssignmentApiController {

    private final MissionService missionService;
    private final UserService userService;
    private final UserMissionAssignmentService userMissionAssignmentService;
    private final PermissionVerifier permissionVerifier;

    public MissionUserAssignmentApiController(
        MissionService missionService,
        UserService userService,
        UserMissionAssignmentService userMissionAssignmentService,
        PermissionVerifier permissionVerifier
    ) {
        this.missionService = missionService;
        this.userService = userService;
        this.userMissionAssignmentService = userMissionAssignmentService;
        this.permissionVerifier = permissionVerifier;
    }

    @GetMapping
    public PageResponse<UserMissionAssignmentResponse> list(
        @AuthenticationPrincipal User currentUser,
        @PathVariable UUID missionId,
        @RequestParam(required = false) UUID userId,
        @PageableDefault(size = 20, sort = "createdAt") Pageable pageable
    ) {
        Mission mission = findMissionOrThrow(missionId);
        requireAccess(currentUser, mission);
        return PageResponse.from(
            userMissionAssignmentService.findByMission(mission, userId, pageable),
            assignment -> UserMissionAssignmentResponse.from(assignment, permissionVerifier.getScopes(mission, currentUser))
        );
    }

    @PostMapping
    public ResponseEntity<?> add(
        @AuthenticationPrincipal User currentUser,
        @PathVariable UUID missionId,
        @RequestBody UserMissionAssignmentRequest request
    ) {
        Mission mission = findMissionOrThrow(missionId);
        requireAccess(currentUser, mission);

        if (request.userId() == null) {
            return ResponseEntity.badRequest().body(new ErrorResponse("User is required."));
        }
        if (request.startTime() != null && request.endTime() != null && request.endTime().isBefore(request.startTime())) {
            return ResponseEntity.badRequest().body(new ErrorResponse("End time cannot be before start time."));
        }

        User user = userService.findById(request.userId()).orElse(null);
        if (user == null) {
            return ResponseEntity.badRequest().body(new ErrorResponse("User not found."));
        }

        UserMissionAssignment assignment = new UserMissionAssignment();
        assignment.setMission(mission);
        assignment.setUser(user);
        assignment.setStartTime(request.startTime());
        assignment.setEndTime(request.endTime());
        userMissionAssignmentService.save(assignment);

        return ResponseEntity.status(HttpStatus.CREATED)
            .body(UserMissionAssignmentResponse.from(assignment, permissionVerifier.getScopes(mission, currentUser)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
        @AuthenticationPrincipal User currentUser,
        @PathVariable UUID missionId,
        @PathVariable UUID id
    ) {
        Mission mission = findMissionOrThrow(missionId);
        requireAccess(currentUser, mission);
        findAssignmentOrThrow(id, mission);

        userMissionAssignmentService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    private UserMissionAssignment findAssignmentOrThrow(UUID id, Mission mission) {
        UserMissionAssignment assignment = userMissionAssignmentService
            .findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!assignment.getMission().getId().equals(mission.getId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return assignment;
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
