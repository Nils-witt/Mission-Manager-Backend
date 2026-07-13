package dev.nilswitt.mission_manager.web;

import dev.nilswitt.mission_manager.data.entities.SecurityGroup;
import dev.nilswitt.mission_manager.data.entities.Tenant;
import dev.nilswitt.mission_manager.data.entities.User;
import dev.nilswitt.mission_manager.data.services.SecurityGroupService;
import dev.nilswitt.mission_manager.data.services.TenantService;
import dev.nilswitt.mission_manager.data.services.UserService;
import dev.nilswitt.mission_manager.security.PermissionVerifier;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static dev.nilswitt.mission_manager.data.entities.SecurityGroup.UserRoleScopeEnum.*;
import static dev.nilswitt.mission_manager.data.entities.SecurityGroup.UserRoleTypeEnum.USER;

@Controller
@RequestMapping("/users")
public class UserManagementController {

    private final UserService userService;
    private final SecurityGroupService securityGroupService;
    private final TenantService tenantService;
    private final PermissionVerifier permissionVerifier;
    private final PasswordEncoder passwordEncoder;

    public UserManagementController(
            UserService userService,
            SecurityGroupService securityGroupService,
            TenantService tenantService,
            PermissionVerifier permissionVerifier,
            PasswordEncoder passwordEncoder
    ) {
        this.userService = userService;
        this.securityGroupService = securityGroupService;
        this.tenantService = tenantService;
        this.permissionVerifier = permissionVerifier;
        this.passwordEncoder = passwordEncoder;
    }

    @GetMapping
    public String list(@AuthenticationPrincipal User currentUser, Model model) {
        requireScope(currentUser, VIEW);
        model.addAttribute("username", currentUser.getUsername());
        model.addAttribute("users", userService.findAll());
        model.addAttribute("canCreate", PermissionVerifier.hasAnyScope(currentUser, USER, CREATE));
        return "users/list";
    }

    @GetMapping("/new")
    public String newForm(@AuthenticationPrincipal User currentUser, Model model) {
        requireScope(currentUser, CREATE);
        model.addAttribute("username", currentUser.getUsername());
        model.addAttribute("form", new UserFormModel());
        model.addAttribute("securityGroups", securityGroupService.findAll());
        model.addAttribute("tenants", tenantService.findAll());
        model.addAttribute("isNew", true);
        return "users/form";
    }

    @PostMapping
    public String create(
            @AuthenticationPrincipal User currentUser,
            @ModelAttribute("form") UserFormModel form,
            Model model
    ) {
        requireScope(currentUser, CREATE);

        if (form.getPassword() == null || form.getPassword().isBlank()) {
            return reRenderForm(model, currentUser, form, true, null, "Password is required for new users.");
        }
        if (form.getPrimaryTenantId() == null) {
            return reRenderForm(model, currentUser, form, true, null, "Primary tenant is required.");
        }

        User user = new User(form.getUsername(), form.getEmail(), form.getFirstName(), form.getLastName());
        user.setPassword(passwordEncoder.encode(form.getPassword()));
        user.setEnabled(form.isEnabled());
        user.setLocked(form.isLocked());
        applySecurityGroups(user, form.getSecurityGroupIds());
        applyPrimaryTenant(user, form.getPrimaryTenantId());

        try {
            userService.save(user);
        } catch (DataIntegrityViolationException ex) {
            return reRenderForm(model, currentUser, form, true, null, "A user with this username or email already exists.");
        }

        return "redirect:/users";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@AuthenticationPrincipal User currentUser, @PathVariable UUID id, Model model) {
        User target = findUserOrThrow(id);
        requireScope(currentUser, EDIT, target);

        UserFormModel form = new UserFormModel();
        form.setUsername(target.getUsername());
        form.setEmail(target.getEmail());
        form.setFirstName(target.getFirstName());
        form.setLastName(target.getLastName());
        form.setEnabled(target.isEnabled());
        form.setLocked(target.isLocked());
        form.setSecurityGroupIds(
                target.getSecurityGroups().stream().map(SecurityGroup::getId).collect(Collectors.toSet())
        );
        // form.setPrimaryTenantId(target.getPrimaryTenant() != null ? target.getPrimaryTenant().getId() : null);

        model.addAttribute("username", currentUser.getUsername());
        model.addAttribute("form", form);
        model.addAttribute("securityGroups", securityGroupService.findAll());
        model.addAttribute("tenants", tenantService.findAll());
        model.addAttribute("isNew", false);
        model.addAttribute("userId", id);
        return "users/form";
    }

    @PostMapping("/{id}")
    public String update(
            @AuthenticationPrincipal User currentUser,
            @PathVariable UUID id,
            @ModelAttribute("form") UserFormModel form,
            Model model
    ) {
        User target = findUserOrThrow(id);
        requireScope(currentUser, EDIT, target);

        if (form.getPrimaryTenantId() == null) {
            return reRenderForm(model, currentUser, form, false, id, "Primary tenant is required.");
        }

        target.setUsername(form.getUsername());
        target.setEmail(form.getEmail());
        target.setFirstName(form.getFirstName());
        target.setLastName(form.getLastName());
        target.setEnabled(form.isEnabled());
        target.setLocked(form.isLocked());
        if (form.getPassword() != null && !form.getPassword().isBlank()) {
            target.setPassword(passwordEncoder.encode(form.getPassword()));
        }
        applySecurityGroups(target, form.getSecurityGroupIds());
        applyPrimaryTenant(target, form.getPrimaryTenantId());

        try {
            userService.save(target);
        } catch (DataIntegrityViolationException ex) {
            return reRenderForm(model, currentUser, form, false, id, "A user with this username or email already exists.");
        }

        return "redirect:/users";
    }

    @PostMapping("/{id}/delete")
    public String delete(@AuthenticationPrincipal User currentUser, @PathVariable UUID id) {
        User target = findUserOrThrow(id);
        requireScope(currentUser, DELETE, target);

        if (target.getId().equals(currentUser.getId())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "You cannot delete your own account.");
        }

        userService.deleteById(id);
        return "redirect:/users";
    }

    private void applySecurityGroups(User user, Set<UUID> groupIds) {
        user.getSecurityGroups().clear();
        if (groupIds != null) {
            groupIds.forEach(groupId -> securityGroupService.findById(groupId).ifPresent(user::addSecurityGroup));
        }
        securityGroupService.findByName("Everyone").ifPresent(user::addSecurityGroup);
    }

    private void applyPrimaryTenant(User user, UUID tenantId) {
        Tenant tenant = tenantService.findById(tenantId).orElse(null);
        user.setPrimaryTenant(tenant);
        if (tenant != null) {
            user.getTenants().add(tenant);
        }
    }

    private User findUserOrThrow(UUID id) {
        return userService.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    private void requireScope(User currentUser, SecurityGroup.UserRoleScopeEnum scope) {
        if (!PermissionVerifier.hasAnyScope(currentUser, USER, scope)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
    }

    private void requireScope(User currentUser, SecurityGroup.UserRoleScopeEnum scope, User target) {
        if (!permissionVerifier.hasAccess(currentUser, scope, target)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
    }

    private String reRenderForm(
            Model model,
            User currentUser,
            UserFormModel form,
            boolean isNew,
            UUID userId,
            String error
    ) {
        model.addAttribute("username", currentUser.getUsername());
        model.addAttribute("form", form);
        model.addAttribute("securityGroups", securityGroupService.findAll());
        model.addAttribute("tenants", tenantService.findAll());
        model.addAttribute("isNew", isNew);
        model.addAttribute("userId", userId);
        model.addAttribute("error", error);
        return "users/form";
    }
}
