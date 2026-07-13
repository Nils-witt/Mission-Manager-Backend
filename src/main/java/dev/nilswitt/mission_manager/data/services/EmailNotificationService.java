package dev.nilswitt.mission_manager.data.services;

import dev.nilswitt.mission_manager.data.entities.SecurityGroup;
import dev.nilswitt.mission_manager.data.entities.Tenant;
import dev.nilswitt.mission_manager.data.entities.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.MailException;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Set;

/**
 * Resolves the recipients for a notification (a single user, everyone in a tenant, or everyone in
 * a security group) and sends each of them the same email via {@link MailService}. A failure to
 * send to one recipient is logged and does not prevent the rest of the batch from going out.
 */
@Slf4j
@Service
public class EmailNotificationService {

    private final MailService mailService;

    public EmailNotificationService(MailService mailService) {
        this.mailService = mailService;
    }

    public void sendToUser(User user, String subject, String body) {
        sendToUsers(Set.of(user), subject, body);
    }

    public void sendToTenant(Tenant tenant, String subject, String body) {
        sendToUsers(tenant.getUsers(), subject, body);
    }

    public void sendToSecurityGroup(SecurityGroup securityGroup, String subject, String body) {
        sendToUsers(securityGroup.getUsers(), subject, body);
    }

    public void sendToUsers(Collection<User> users, String subject, String body) {
        if (users == null) {
            return;
        }
        for (User user : users) {
            String email = user.getEmail();
            if (email == null || email.isBlank()) {
                log.warn("Skipping email to user '{}': no email address on file", user.getUsername());
                continue;
            }
            try {
                mailService.send(email, subject, body);
            } catch (MailException e) {
                log.error("Failed to send email to '{}'", email, e);
            }
        }
    }
}
