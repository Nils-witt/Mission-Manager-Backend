package dev.nilswitt.mission_manager.web;

import dev.nilswitt.mission_manager.data.entities.Mission;
import dev.nilswitt.mission_manager.data.entities.SecurityGroup;
import dev.nilswitt.mission_manager.data.entities.User;
import dev.nilswitt.mission_manager.data.services.MissionService;
import dev.nilswitt.mission_manager.data.services.TenantService;
import dev.nilswitt.mission_manager.security.PermissionVerifier;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

import static dev.nilswitt.mission_manager.data.entities.SecurityGroup.UserRoleScopeEnum.CREATE;
import static dev.nilswitt.mission_manager.data.entities.SecurityGroup.UserRoleScopeEnum.DELETE;
import static dev.nilswitt.mission_manager.data.entities.SecurityGroup.UserRoleScopeEnum.EDIT;
import static dev.nilswitt.mission_manager.data.entities.SecurityGroup.UserRoleScopeEnum.VIEW;
import static dev.nilswitt.mission_manager.data.entities.SecurityGroup.UserRoleTypeEnum.MISSION;

@Controller
@RequestMapping("/missions")
public class MissionManagementController {

    private final MissionService missionService;
    private final TenantService tenantService;
    private final PermissionVerifier permissionVerifier;

    public MissionManagementController(
        MissionService missionService,
        TenantService tenantService,
        PermissionVerifier permissionVerifier
    ) {
        this.missionService = missionService;
        this.tenantService = tenantService;
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

        Mission mission = new Mission();
        mission.setName(form.getName());
        mission.setTenant(tenantService.findById(form.getTenantId()).orElse(null));

        try {
            missionService.save(mission);
        } catch (DataIntegrityViolationException ex) {
            return reRenderForm(model, currentUser, form, true, null, "Unable to save this mission.");
        }

        return "redirect:/missions";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@AuthenticationPrincipal User currentUser, @PathVariable UUID id, Model model) {
        Mission target = findMissionOrThrow(id);
        requireScope(currentUser, EDIT, target);

        MissionFormModel form = new MissionFormModel();
        form.setName(target.getName());
        form.setTenantId(target.getTenant().getId());

        model.addAttribute("username", currentUser.getUsername());
        model.addAttribute("form", form);
        model.addAttribute("tenants", tenantService.findAll());
        model.addAttribute("isNew", false);
        model.addAttribute("missionId", id);
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

        target.setName(form.getName());
        target.setTenant(tenantService.findById(form.getTenantId()).orElse(null));

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
        return "missions/form";
    }
}
