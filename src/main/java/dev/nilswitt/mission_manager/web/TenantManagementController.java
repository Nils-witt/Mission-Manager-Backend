package dev.nilswitt.mission_manager.web;

import dev.nilswitt.mission_manager.data.entities.SecurityGroup;
import dev.nilswitt.mission_manager.data.entities.Tenant;
import dev.nilswitt.mission_manager.data.entities.User;
import dev.nilswitt.mission_manager.data.services.TenantService;
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
import static dev.nilswitt.mission_manager.data.entities.SecurityGroup.UserRoleTypeEnum.SECURITYGROUP;

@Controller
@RequestMapping("/tenants")
public class TenantManagementController {

    private final TenantService tenantService;

    public TenantManagementController(TenantService tenantService) {
        this.tenantService = tenantService;
    }

    @GetMapping
    public String list(@AuthenticationPrincipal User currentUser, Model model) {
        requireScope(currentUser, VIEW);
        model.addAttribute("username", currentUser.getUsername());
        model.addAttribute("tenants", tenantService.findAll());
        model.addAttribute("canCreate", PermissionVerifier.hasAnyScope(currentUser, SECURITYGROUP, CREATE));
        return "tenants/list";
    }

    @GetMapping("/new")
    public String newForm(@AuthenticationPrincipal User currentUser, Model model) {
        requireScope(currentUser, CREATE);
        model.addAttribute("username", currentUser.getUsername());
        model.addAttribute("form", new TenantFormModel());
        model.addAttribute("types", SecurityGroup.UserRoleTypeEnum.values());
        model.addAttribute("scopes", SecurityGroup.UserRoleScopeEnum.values());
        model.addAttribute("isNew", true);
        return "tenants/form";
    }

    @PostMapping
    public String create(
            @AuthenticationPrincipal User currentUser,
            @ModelAttribute("form") TenantFormModel form,
            Model model
    ) {
        requireScope(currentUser, CREATE);

        Tenant tenant = new Tenant(form.getName());

        try {
            tenantService.save(tenant);
        } catch (DataIntegrityViolationException ex) {
            return reRenderForm(model, currentUser, form, true, null, "A tenant with this name already exists.");
        }

        return "redirect:/tenants";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@AuthenticationPrincipal User currentUser, @PathVariable UUID id, Model model) {
        requireScope(currentUser, EDIT);
        Tenant target = findTenantOrThrow(id);

        TenantFormModel form = new TenantFormModel();
        form.setName(target.getName());

        model.addAttribute("username", currentUser.getUsername());
        model.addAttribute("form", form);
        model.addAttribute("types", SecurityGroup.UserRoleTypeEnum.values());
        model.addAttribute("scopes", SecurityGroup.UserRoleScopeEnum.values());
        model.addAttribute("isNew", false);
        model.addAttribute("tenantId", id);
        return "tenants/form";
    }

    @PostMapping("/{id}")
    public String update(
            @AuthenticationPrincipal User currentUser,
            @PathVariable UUID id,
            @ModelAttribute("form") TenantFormModel form,
            Model model
    ) {
        requireScope(currentUser, EDIT);
        Tenant target = findTenantOrThrow(id);

        target.setName(form.getName());

        try {
            tenantService.save(target);
        } catch (DataIntegrityViolationException ex) {
            return reRenderForm(model, currentUser, form, false, id, "A tenant with this name already exists.");
        }

        return "redirect:/tenants";
    }

    @PostMapping("/{id}/delete")
    public String delete(@AuthenticationPrincipal User currentUser, @PathVariable UUID id) {
        requireScope(currentUser, DELETE);
        Tenant target = findTenantOrThrow(id);

        tenantService.deleteWithCleanup(target);
        return "redirect:/tenants";
    }

    private Tenant findTenantOrThrow(UUID id) {
        return tenantService.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    private void requireScope(User currentUser, SecurityGroup.UserRoleScopeEnum scope) {
        if (!PermissionVerifier.hasAnyScope(currentUser, SECURITYGROUP, scope)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
    }

    private String reRenderForm(
            Model model,
            User currentUser,
            TenantFormModel form,
            boolean isNew,
            UUID tenantId,
            String error
    ) {
        model.addAttribute("username", currentUser.getUsername());
        model.addAttribute("form", form);
        model.addAttribute("types", SecurityGroup.UserRoleTypeEnum.values());
        model.addAttribute("scopes", SecurityGroup.UserRoleScopeEnum.values());
        model.addAttribute("isNew", isNew);
        model.addAttribute("tenantId", tenantId);
        model.addAttribute("error", error);
        return "tenants/form";
    }
}
