package dev.nilswitt.mission_manager.web;

import dev.nilswitt.mission_manager.data.entities.Mission;
import dev.nilswitt.mission_manager.data.entities.User;
import dev.nilswitt.mission_manager.data.entities.UserPosition;
import dev.nilswitt.mission_manager.data.services.MissionService;
import dev.nilswitt.mission_manager.data.services.QualificationService;
import dev.nilswitt.mission_manager.data.services.UserPositionService;
import dev.nilswitt.mission_manager.data.services.UserService;
import dev.nilswitt.mission_manager.security.PermissionVerifier;
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
import java.util.Set;
import java.util.UUID;

import static dev.nilswitt.mission_manager.data.entities.SecurityGroup.UserRoleScopeEnum.EDIT;

@Controller
@RequestMapping("/missions/{missionId}/positions")
public class MissionPositionController {

    private final MissionService missionService;
    private final UserPositionService userPositionService;
    private final QualificationService qualificationService;
    private final UserService userService;
    private final PermissionVerifier permissionVerifier;

    public MissionPositionController(
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

    @PostMapping
    public String add(
        @AuthenticationPrincipal User currentUser,
        @PathVariable UUID missionId,
        @RequestParam String name,
        @RequestParam(required = false) Set<UUID> qualificationIds,
        @RequestParam(required = false) UUID assignedUserId
    ) {
        Mission mission = findMissionOrThrow(missionId);
        requireAccess(currentUser, mission);

        if (name == null || name.isBlank()) {
            return redirectWithError(missionId, "Name is required.");
        }

        UserPosition position = new UserPosition();
        position.setMission(mission);
        position.setName(name);
        applyQualifications(position, qualificationIds);
        applyAssignedUser(position, assignedUserId);

        userPositionService.save(position);

        return "redirect:/missions/" + missionId + "/edit";
    }

    @PostMapping("/{id}/assigned-user")
    public String updateAssignedUser(
        @AuthenticationPrincipal User currentUser,
        @PathVariable UUID missionId,
        @PathVariable UUID id,
        @RequestParam(required = false) UUID assignedUserId
    ) {
        Mission mission = findMissionOrThrow(missionId);
        requireAccess(currentUser, mission);
        UserPosition position = findPositionOrThrow(id, mission);

        applyAssignedUser(position, assignedUserId);
        userPositionService.save(position);

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
        findPositionOrThrow(id, mission);

        userPositionService.deleteById(id);
        return "redirect:/missions/" + missionId + "/edit";
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

    private String redirectWithError(UUID missionId, String message) {
        return "redirect:/missions/" + missionId + "/edit?positionError=" + UriUtils.encode(message, StandardCharsets.UTF_8);
    }
}
