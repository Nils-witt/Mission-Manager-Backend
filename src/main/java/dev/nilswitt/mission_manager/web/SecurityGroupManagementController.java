package dev.nilswitt.mission_manager.web;

import dev.nilswitt.mission_manager.data.entities.SecurityGroup;
import dev.nilswitt.mission_manager.data.entities.User;
import dev.nilswitt.mission_manager.data.services.SecurityGroupService;
import dev.nilswitt.mission_manager.security.PermissionVerifier;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashSet;
import java.util.UUID;

import static dev.nilswitt.mission_manager.data.entities.SecurityGroup.UserRoleScopeEnum.*;
import static dev.nilswitt.mission_manager.data.entities.SecurityGroup.UserRoleTypeEnum.SECURITYGROUP;

@Controller
@RequestMapping("/security-groups")
public class SecurityGroupManagementController {

    private final SecurityGroupService securityGroupService;

    public SecurityGroupManagementController(SecurityGroupService securityGroupService) {
        this.securityGroupService = securityGroupService;
    }

    @GetMapping
    public String list(@AuthenticationPrincipal User currentUser, Model model) {
        requireScope(currentUser, VIEW);
        model.addAttribute("username", currentUser.getUsername());
        model.addAttribute(
                "groups",
                securityGroupService.findAll().stream().toList()
        );
        model.addAttribute("canCreate", PermissionVerifier.hasAnyScope(currentUser, SECURITYGROUP, CREATE));
        return "security-groups/list";
    }

    @GetMapping("/new")
    public String newForm(@AuthenticationPrincipal User currentUser, Model model) {
        requireScope(currentUser, CREATE);
        model.addAttribute("username", currentUser.getUsername());
        model.addAttribute("form", new SecurityGroupFormModel());
        model.addAttribute("types", SecurityGroup.UserRoleTypeEnum.values());
        model.addAttribute("scopes", SecurityGroup.UserRoleScopeEnum.values());
        model.addAttribute("isNew", true);
        model.addAttribute("isBuiltIn", false);
        return "security-groups/form";
    }

    @PostMapping
    public String create(
            @AuthenticationPrincipal User currentUser,
            @ModelAttribute("form") SecurityGroupFormModel form,
            Model model
    ) {
        requireScope(currentUser, CREATE);

        SecurityGroup group = new SecurityGroup(form.getName(), new HashSet<>(form.getRoles()));
        group.setSsoGroupName(form.getSsoGroupName() == null ? "" : form.getSsoGroupName());

        try {
            securityGroupService.save(group);
        } catch (DataIntegrityViolationException ex) {
            return reRenderForm(model, currentUser, form, true, null, false, "A security group with this name already exists.");
        }

        return "redirect:/security-groups";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@AuthenticationPrincipal User currentUser, @PathVariable UUID id, Model model) {
        requireScope(currentUser, EDIT);
        SecurityGroup target = findGroupOrThrow(id);

        SecurityGroupFormModel form = new SecurityGroupFormModel();
        form.setName(target.getName());
        form.setSsoGroupName(target.getSsoGroupName());
        form.setRoles(new HashSet<>(target.getRoles()));

        model.addAttribute("username", currentUser.getUsername());
        model.addAttribute("form", form);
        model.addAttribute("types", SecurityGroup.UserRoleTypeEnum.values());
        model.addAttribute("scopes", SecurityGroup.UserRoleScopeEnum.values());
        model.addAttribute("isNew", false);
        model.addAttribute("groupId", id);
        model.addAttribute("isBuiltIn", target.isBuiltIn());
        return "security-groups/form";
    }

    @PostMapping("/{id}")
    public String update(
            @AuthenticationPrincipal User currentUser,
            @PathVariable UUID id,
            @ModelAttribute("form") SecurityGroupFormModel form,
            Model model
    ) {
        requireScope(currentUser, EDIT);
        SecurityGroup target = findGroupOrThrow(id);

        if (target.isBuiltIn()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Built-in security groups cannot be edited.");
        }

        target.setName(form.getName());
        target.setSsoGroupName(form.getSsoGroupName() == null ? "" : form.getSsoGroupName());
        target.setRoles(new HashSet<>(form.getRoles()));

        try {
            securityGroupService.save(target);
        } catch (DataIntegrityViolationException ex) {
            return reRenderForm(model, currentUser, form, false, id, false, "A security group with this name already exists.");
        }

        return "redirect:/security-groups";
    }

    @PostMapping("/{id}/delete")
    public String delete(@AuthenticationPrincipal User currentUser, @PathVariable UUID id) {
        requireScope(currentUser, DELETE);
        SecurityGroup target = findGroupOrThrow(id);

        if (target.isBuiltIn()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Built-in security groups cannot be deleted.");
        }

        securityGroupService.deleteWithCleanup(target);
        return "redirect:/security-groups";
    }

    private SecurityGroup findGroupOrThrow(UUID id) {
        return securityGroupService.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    private void requireScope(User currentUser, SecurityGroup.UserRoleScopeEnum scope) {
        if (!PermissionVerifier.hasAnyScope(currentUser, SECURITYGROUP, scope)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
    }

    private String reRenderForm(
            Model model,
            User currentUser,
            SecurityGroupFormModel form,
            boolean isNew,
            UUID groupId,
            boolean isBuiltIn,
            String error
    ) {
        model.addAttribute("username", currentUser.getUsername());
        model.addAttribute("form", form);
        model.addAttribute("types", SecurityGroup.UserRoleTypeEnum.values());
        model.addAttribute("scopes", SecurityGroup.UserRoleScopeEnum.values());
        model.addAttribute("isNew", isNew);
        model.addAttribute("groupId", groupId);
        model.addAttribute("isBuiltIn", isBuiltIn);
        model.addAttribute("error", error);
        return "security-groups/form";
    }
}
