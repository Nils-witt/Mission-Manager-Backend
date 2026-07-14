package dev.nilswitt.mission_manager.web;

import dev.nilswitt.mission_manager.data.entities.Mission;
import dev.nilswitt.mission_manager.data.entities.User;
import dev.nilswitt.mission_manager.data.entities.UserMissionAssignment;
import dev.nilswitt.mission_manager.data.services.MissionService;
import dev.nilswitt.mission_manager.data.services.UserMissionAssignmentService;
import dev.nilswitt.mission_manager.data.services.UserService;
import dev.nilswitt.mission_manager.security.PermissionVerifier;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.UUID;

import static dev.nilswitt.mission_manager.data.entities.SecurityGroup.UserRoleScopeEnum.EDIT;

@Controller
@RequestMapping("/missions/{missionId}/users")
public class MissionUserAssignmentController {

    private final MissionService missionService;
    private final UserService userService;
    private final UserMissionAssignmentService userMissionAssignmentService;
    private final PermissionVerifier permissionVerifier;

    public MissionUserAssignmentController(
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

    @PostMapping
    public String add(
        @AuthenticationPrincipal User currentUser,
        @PathVariable UUID missionId,
        @RequestParam(required = false) UUID userId,
        @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm") LocalDateTime startTime,
        @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm") LocalDateTime endTime
    ) {
        Mission mission = findMissionOrThrow(missionId);
        requireAccess(currentUser, mission);

        if (userId == null) {
            return redirectWithError(missionId, "User is required.");
        }
        if (startTime != null && endTime != null && endTime.isBefore(startTime)) {
            return redirectWithError(missionId, "End time cannot be before start time.");
        }

        User user = userService.findById(userId).orElse(null);
        if (user == null) {
            return redirectWithError(missionId, "User not found.");
        }

        UserMissionAssignment assignment = new UserMissionAssignment();
        assignment.setMission(mission);
        assignment.setUser(user);
        assignment.setStartTime(startTime);
        assignment.setEndTime(endTime);
        userMissionAssignmentService.save(assignment);

        return "redirect:/missions/" + missionId + "/edit";
    }

    @PostMapping("/{id}/delete")
    public String delete(
        @AuthenticationPrincipal User currentUser,
        @PathVariable UUID missionId,
        @PathVariable UUID id
    ) {
        Mission mission = findMissionOrThrow(missionId);
        requireAccess(currentUser, mission);
        findAssignmentOrThrow(id, mission);

        userMissionAssignmentService.deleteById(id);
        return "redirect:/missions/" + missionId + "/edit";
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

    private String redirectWithError(UUID missionId, String message) {
        return "redirect:/missions/" + missionId + "/edit?assignmentError=" + UriUtils.encode(message, StandardCharsets.UTF_8);
    }
}
