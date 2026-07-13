package dev.nilswitt.mission_manager.web;

import dev.nilswitt.mission_manager.data.entities.SecurityGroup;
import dev.nilswitt.mission_manager.data.entities.Tenant;
import dev.nilswitt.mission_manager.data.entities.User;
import dev.nilswitt.mission_manager.data.services.EmailNotificationService;
import dev.nilswitt.mission_manager.data.services.SecurityGroupService;
import dev.nilswitt.mission_manager.data.services.TenantService;
import dev.nilswitt.mission_manager.data.services.UserService;
import dev.nilswitt.mission_manager.security.PermissionVerifier;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

import java.util.Set;
import java.util.UUID;

import static dev.nilswitt.mission_manager.data.entities.SecurityGroup.UserRoleScopeEnum.CREATE;
import static dev.nilswitt.mission_manager.data.entities.SecurityGroup.UserRoleTypeEnum.EMAIL;

@Controller
@RequestMapping("/emails")
public class EmailComposeController {

    private static final Set<String> RECIPIENT_TYPES = Set.of("USER", "TENANT", "GROUP");

    private final UserService userService;
    private final TenantService tenantService;
    private final SecurityGroupService securityGroupService;
    private final EmailNotificationService emailNotificationService;

    public EmailComposeController(
        UserService userService,
        TenantService tenantService,
        SecurityGroupService securityGroupService,
        EmailNotificationService emailNotificationService
    ) {
        this.userService = userService;
        this.tenantService = tenantService;
        this.securityGroupService = securityGroupService;
        this.emailNotificationService = emailNotificationService;
    }

    @GetMapping
    public String composeForm(
        @AuthenticationPrincipal User currentUser,
        @RequestParam(required = false) String recipientType,
        @RequestParam(required = false) UUID recipientId,
        @RequestParam(required = false) String sent,
        Model model
    ) {
        requireScope(currentUser);

        EmailComposeFormModel form = new EmailComposeFormModel();
        boolean lockRecipient = false;
        String recipientLabel = null;

        if (recipientType != null && recipientId != null) {
            recipientLabel = resolveLabel(recipientType, recipientId);
            if (recipientLabel != null) {
                form.setRecipientType(recipientType);
                form.setRecipientId(recipientId);
                lockRecipient = true;
            }
        }

        populateForm(model, currentUser, form, lockRecipient, recipientLabel, sent != null, null);
        return "emails/compose";
    }

    @PostMapping
    public String send(
        @AuthenticationPrincipal User currentUser,
        @ModelAttribute("form") EmailComposeFormModel form,
        Model model
    ) {
        requireScope(currentUser);

        if (form.getRecipientType() == null || !RECIPIENT_TYPES.contains(form.getRecipientType()) || form.getRecipientId() == null) {
            return reRenderForm(model, currentUser, form, "Please choose a recipient.");
        }
        if (form.getSubject() == null || form.getSubject().isBlank()) {
            return reRenderForm(model, currentUser, form, "Subject is required.");
        }
        if (form.getBody() == null || form.getBody().isBlank()) {
            return reRenderForm(model, currentUser, form, "Message body is required.");
        }

        boolean dispatched =
            switch (form.getRecipientType()) {
                case "USER" -> userService
                    .findById(form.getRecipientId())
                    .map(user -> {
                        emailNotificationService.sendToUser(user, form.getSubject(), form.getBody());
                        return true;
                    })
                    .orElse(false);
                case "TENANT" -> tenantService
                    .findById(form.getRecipientId())
                    .map(tenant -> {
                        emailNotificationService.sendToTenant(tenant, form.getSubject(), form.getBody());
                        return true;
                    })
                    .orElse(false);
                case "GROUP" -> securityGroupService
                    .findById(form.getRecipientId())
                    .map(group -> {
                        emailNotificationService.sendToSecurityGroup(group, form.getSubject(), form.getBody());
                        return true;
                    })
                    .orElse(false);
                default -> false;
            };

        if (!dispatched) {
            return reRenderForm(model, currentUser, form, "Recipient could not be found.");
        }

        return "redirect:/emails?sent=1";
    }

    private String resolveLabel(String recipientType, UUID recipientId) {
        return switch (recipientType) {
            case "USER" -> userService.findById(recipientId).map(u -> "User: " + u.getUsername()).orElse(null);
            case "TENANT" -> tenantService
                .findById(recipientId)
                .map(t -> "All members of tenant \"" + t.getName() + "\"")
                .orElse(null);
            case "GROUP" -> securityGroupService
                .findById(recipientId)
                .map(g -> "All members of security group \"" + g.getName() + "\"")
                .orElse(null);
            default -> null;
        };
    }

    private void requireScope(User currentUser) {
        if (!PermissionVerifier.hasAnyScope(currentUser, EMAIL, CREATE)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
    }

    private String reRenderForm(Model model, User currentUser, EmailComposeFormModel form, String error) {
        boolean lockRecipient = false;
        String recipientLabel = null;
        if (form.getRecipientType() != null && form.getRecipientId() != null) {
            recipientLabel = resolveLabel(form.getRecipientType(), form.getRecipientId());
            lockRecipient = recipientLabel != null;
        }
        populateForm(model, currentUser, form, lockRecipient, recipientLabel, false, error);
        return "emails/compose";
    }

    private void populateForm(
        Model model,
        User currentUser,
        EmailComposeFormModel form,
        boolean lockRecipient,
        String recipientLabel,
        boolean sent,
        String error
    ) {
        model.addAttribute("username", currentUser.getUsername());
        model.addAttribute("form", form);
        model.addAttribute("users", userService.findAll());
        model.addAttribute("tenants", tenantService.findAll());
        model.addAttribute("groups", securityGroupService.findAll());
        model.addAttribute("lockRecipient", lockRecipient);
        model.addAttribute("recipientLabel", recipientLabel);
        model.addAttribute("sent", sent);
        model.addAttribute("error", error);
    }
}
