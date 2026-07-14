package dev.nilswitt.mission_manager.api;

import dev.nilswitt.mission_manager.api.dto.EmailRequest;
import dev.nilswitt.mission_manager.api.dto.ErrorResponse;
import dev.nilswitt.mission_manager.data.entities.User;
import dev.nilswitt.mission_manager.data.services.EmailNotificationService;
import dev.nilswitt.mission_manager.data.services.SecurityGroupService;
import dev.nilswitt.mission_manager.data.services.TenantService;
import dev.nilswitt.mission_manager.data.services.UserService;
import dev.nilswitt.mission_manager.security.PermissionVerifier;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Set;

import static dev.nilswitt.mission_manager.data.entities.SecurityGroup.UserRoleScopeEnum.CREATE;
import static dev.nilswitt.mission_manager.data.entities.SecurityGroup.UserRoleTypeEnum.EMAIL;

@RestController
@RequestMapping("/api/emails")
@Tag(name = "Emails", description = "Send email notifications to a user, a tenant, or a security group")
public class EmailApiController {

    private static final Set<String> RECIPIENT_TYPES = Set.of("USER", "TENANT", "GROUP");

    private final UserService userService;
    private final TenantService tenantService;
    private final SecurityGroupService securityGroupService;
    private final EmailNotificationService emailNotificationService;

    public EmailApiController(
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

    @PostMapping
    public ResponseEntity<?> send(@AuthenticationPrincipal User currentUser, @RequestBody EmailRequest request) {
        if (!PermissionVerifier.hasAnyScope(currentUser, EMAIL, CREATE)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        if (request.recipientType() == null || !RECIPIENT_TYPES.contains(request.recipientType()) || request.recipientId() == null) {
            return ResponseEntity.badRequest().body(new ErrorResponse("recipientType must be USER, TENANT, or GROUP and recipientId is required."));
        }
        if (request.subject() == null || request.subject().isBlank()) {
            return ResponseEntity.badRequest().body(new ErrorResponse("subject is required."));
        }
        if (request.body() == null || request.body().isBlank()) {
            return ResponseEntity.badRequest().body(new ErrorResponse("body is required."));
        }

        boolean dispatched =
            switch (request.recipientType()) {
                case "USER" -> userService
                    .findById(request.recipientId())
                    .map(user -> {
                        emailNotificationService.sendToUser(user, request.subject(), request.body());
                        return true;
                    })
                    .orElse(false);
                case "TENANT" -> tenantService
                    .findById(request.recipientId())
                    .map(tenant -> {
                        emailNotificationService.sendToTenant(tenant, request.subject(), request.body());
                        return true;
                    })
                    .orElse(false);
                case "GROUP" -> securityGroupService
                    .findById(request.recipientId())
                    .map(group -> {
                        emailNotificationService.sendToSecurityGroup(group, request.subject(), request.body());
                        return true;
                    })
                    .orElse(false);
                default -> false;
            };

        if (!dispatched) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(new ErrorResponse("Recipient could not be found."));
        }

        return ResponseEntity.accepted().build();
    }
}
