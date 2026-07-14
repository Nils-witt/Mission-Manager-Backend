package dev.nilswitt.mission_manager.web;

import dev.nilswitt.mission_manager.data.entities.Mission;
import dev.nilswitt.mission_manager.data.entities.SecurityGroup;
import dev.nilswitt.mission_manager.data.entities.User;
import dev.nilswitt.mission_manager.data.services.MissionService;
import dev.nilswitt.mission_manager.data.services.QualificationService;
import dev.nilswitt.mission_manager.data.services.TenantService;
import dev.nilswitt.mission_manager.data.services.UserMissionAssignmentService;
import dev.nilswitt.mission_manager.data.services.UserPositionService;
import dev.nilswitt.mission_manager.data.services.UserService;
import dev.nilswitt.mission_manager.security.PermissionVerifier;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

import static dev.nilswitt.mission_manager.data.entities.SecurityGroup.UserRoleScopeEnum.*;
import static dev.nilswitt.mission_manager.data.entities.SecurityGroup.UserRoleTypeEnum.MISSION;

@Controller
@RequestMapping("/missions")
public class MissionManagementController {

    private final MissionService missionService;
    private final TenantService tenantService;
    private final UserService userService;
    private final UserMissionAssignmentService userMissionAssignmentService;
    private final UserPositionService userPositionService;
    private final QualificationService qualificationService;
    private final PermissionVerifier permissionVerifier;

    public MissionManagementController(
            MissionService missionService,
            TenantService tenantService,
            UserService userService,
            UserMissionAssignmentService userMissionAssignmentService,
            UserPositionService userPositionService,
            QualificationService qualificationService,
            PermissionVerifier permissionVerifier
    ) {
        this.missionService = missionService;
        this.tenantService = tenantService;
        this.userService = userService;
        this.userMissionAssignmentService = userMissionAssignmentService;
        this.userPositionService = userPositionService;
        this.qualificationService = qualificationService;
        this.permissionVerifier = permissionVerifier;
    }

    @GetMapping
    public String list(@AuthenticationPrincipal User currentUser, @RequestParam(required = false) String error, Model model) {
        requireScope(currentUser, VIEW);
        model.addAttribute("username", currentUser.getUsername());
        model.addAttribute("missions", missionService.findAll());
        model.addAttribute("canCreate", PermissionVerifier.hasAnyScope(currentUser, MISSION, CREATE));
        model.addAttribute("error", error);
        return "missions/list";
    }

    @GetMapping("/new")
    public String newForm(@AuthenticationPrincipal User currentUser, Model model) {
        requireScope(currentUser, CREATE);
        model.addAttribute("username", currentUser.getUsername());
        model.addAttribute("form", new MissionFormModel());
        model.addAttribute("tenants", tenantService.findAll());
        model.addAttribute("isNew", true);
        return "missions/form";
    }

    @PostMapping
    public String create(
            @AuthenticationPrincipal User currentUser,
            @ModelAttribute("form") MissionFormModel form,
            Model model
    ) {
        requireScope(currentUser, CREATE);

        if (form.getTenantId() == null) {
            return reRenderForm(model, currentUser, form, true, null, "Tenant is required.");
        }
        if (isEndBeforeStart(form)) {
            return reRenderForm(model, currentUser, form, true, null, "End time cannot be before start time.");
        }

        Mission mission = new Mission();
        mission.setName(form.getName());
        mission.setTenant(tenantService.findById(form.getTenantId()).orElse(null));
        applyDetails(mission, form);

        try {
            missionService.save(mission);
        } catch (DataIntegrityViolationException ex) {
            return reRenderForm(model, currentUser, form, true, null, "Unable to save this mission.");
        }

        return "redirect:/missions";
    }

    @GetMapping("/{id}/edit")
    public String editForm(
            @AuthenticationPrincipal User currentUser,
            @PathVariable UUID id,
            @RequestParam(required = false) String assignmentError,
            @RequestParam(required = false) String positionError,
            Model model
    ) {
        Mission target = findMissionOrThrow(id);
        requireScope(currentUser, EDIT, target);

        MissionFormModel form = new MissionFormModel();
        form.setName(target.getName());
        form.setTenantId(target.getTenant().getId());
        form.setStartTime(target.getStartTime());
        form.setEndTime(target.getEndTime());
        form.setLatitude(target.getLatitude());
        form.setLongitude(target.getLongitude());
        form.setStreetAddress(target.getStreetAddress());

        model.addAttribute("username", currentUser.getUsername());
        model.addAttribute("form", form);
        model.addAttribute("tenants", tenantService.findAll());
        model.addAttribute("isNew", false);
        model.addAttribute("missionId", id);
        model.addAttribute("users", userService.findAll());
        model.addAttribute("missionAssignments", userMissionAssignmentService.findByMission(target));
        model.addAttribute("assignmentError", assignmentError);
        model.addAttribute("positions", userPositionService.findByMission(target));
        model.addAttribute("qualifications", qualificationService.findAll());
        model.addAttribute("positionError", positionError);
        return "missions/form";
    }

    @PostMapping("/{id}")
    public String update(
            @AuthenticationPrincipal User currentUser,
            @PathVariable UUID id,
            @ModelAttribute("form") MissionFormModel form,
            Model model
    ) {
        Mission target = findMissionOrThrow(id);
        requireScope(currentUser, EDIT, target);

        if (form.getTenantId() == null) {
            return reRenderForm(model, currentUser, form, false, id, "Tenant is required.");
        }
        if (isEndBeforeStart(form)) {
            return reRenderForm(model, currentUser, form, false, id, "End time cannot be before start time.");
        }

        target.setName(form.getName());
        target.setTenant(tenantService.findById(form.getTenantId()).orElse(null));
        applyDetails(target, form);

        try {
            missionService.save(target);
        } catch (DataIntegrityViolationException ex) {
            return reRenderForm(model, currentUser, form, false, id, "Unable to save this mission.");
        }

        return "redirect:/missions";
    }

    @PostMapping("/{id}/delete")
    public String delete(@AuthenticationPrincipal User currentUser, @PathVariable UUID id) {
        Mission target = findMissionOrThrow(id);
        requireScope(currentUser, DELETE, target);

        try {
            missionService.deleteById(id);
        } catch (DataIntegrityViolationException ex) {
            return "redirect:/missions?error=Cannot delete a mission that still has permissions referencing it.";
        }

        return "redirect:/missions";
    }

    private Mission findMissionOrThrow(UUID id) {
        return missionService.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    private void applyDetails(Mission mission, MissionFormModel form) {
        mission.setStartTime(form.getStartTime());
        mission.setEndTime(form.getEndTime());
        mission.setLatitude(form.getLatitude());
        mission.setLongitude(form.getLongitude());
        mission.setStreetAddress(form.getStreetAddress());
    }

    private boolean isEndBeforeStart(MissionFormModel form) {
        return form.getStartTime() != null && form.getEndTime() != null && form.getEndTime().isBefore(form.getStartTime());
    }

    private void requireScope(User currentUser, SecurityGroup.UserRoleScopeEnum scope) {
        if (!PermissionVerifier.hasAnyScope(currentUser, MISSION, scope)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
    }

    private void requireScope(User currentUser, SecurityGroup.UserRoleScopeEnum scope, Mission target) {
        if (!permissionVerifier.hasAccess(currentUser, scope, target)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
    }

    private String reRenderForm(
            Model model,
            User currentUser,
            MissionFormModel form,
            boolean isNew,
            UUID missionId,
            String error
    ) {
        model.addAttribute("username", currentUser.getUsername());
        model.addAttribute("form", form);
        model.addAttribute("tenants", tenantService.findAll());
        model.addAttribute("isNew", isNew);
        model.addAttribute("missionId", missionId);
        model.addAttribute("error", error);
        if (!isNew && missionId != null) {
            model.addAttribute("users", userService.findAll());
            model.addAttribute("qualifications", qualificationService.findAll());
            missionService.findById(missionId)
                    .ifPresent(mission -> {
                        model.addAttribute("missionAssignments", userMissionAssignmentService.findByMission(mission));
                        model.addAttribute("positions", userPositionService.findByMission(mission));
                    });
        }
        return "missions/form";
    }
}
