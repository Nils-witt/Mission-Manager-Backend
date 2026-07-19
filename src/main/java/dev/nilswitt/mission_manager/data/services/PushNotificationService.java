package dev.nilswitt.mission_manager.data.services;

import dev.nilswitt.mission_manager.data.entities.NotificationDestination;
import dev.nilswitt.mission_manager.data.entities.SecurityGroup;
import dev.nilswitt.mission_manager.data.entities.Tenant;
import dev.nilswitt.mission_manager.data.entities.User;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.List;

/**
 * Resolves the recipients for a push notification (a single user, everyone in a tenant, or
 * everyone in a security group) and sends it to each of their registered devices. A failure to
 * send to one device is logged and does not prevent the rest of the batch from going out; devices
 * whose token APNs reports as no longer valid are deregistered automatically.
 */
@Slf4j
@Service
public class PushNotificationService {

    private final NotificationDestinationService notificationDestinationService;
    private final ApnsService apnsService;

    public PushNotificationService(NotificationDestinationService notificationDestinationService, ApnsService apnsService) {
        this.notificationDestinationService = notificationDestinationService;
        this.apnsService = apnsService;
    }

    public void sendToUser(User user, String title, String body) {
        for (NotificationDestination destination : notificationDestinationService.findByUser(user)) {
            sendToDestination(destination, title, body);
        }
    }

    public void sendToTenant(Tenant tenant, String title, String body) {
        sendToUsers(tenant.getUsers(), title, body);
    }

    public void sendToSecurityGroup(SecurityGroup securityGroup, String title, String body) {
        sendToUsers(securityGroup.getUsers(), title, body);
    }

    public void sendToUsers(Collection<User> users, String title, String body) {
        if (users == null) {
            return;
        }
        for (User user : users) {
            sendToUser(user, title, body);
        }
    }

    public void sendToDestination(NotificationDestination destination, String title, String body) {
        if (destination.getDeviceType() != NotificationDestination.DeviceTypeEnum.IOS) {
            log.debug(
                    "Skipping push to destination '{}': device type '{}' has no push transport yet",
                    destination.getId(),
                    destination.getDeviceType()
            );
            return;
        }

        ApnsService.PushResult result = apnsService.send(destination.getToken(), title, body);
        if (!result.accepted() && result.tokenInvalid()) {
            log.info("Deregistering notification destination '{}': APNs reports the token is no longer valid ({})",
                    destination.getId(), result.rejectionReason());
            notificationDestinationService.deleteById(destination.getId());
        }
    }

    public void sendMissionUpdate(String missionId) {

        List<NotificationDestination> destinations = notificationDestinationService.findByType(NotificationDestination.DeviceTypeEnum.IOS);
        log.info("Sending notifications for mission id '{}' '{}' devices", missionId, destinations.size());
        for (NotificationDestination destination : destinations) {
            ApnsService.PushResult result = apnsService.sendBGPushUpdatedMission(destination.getToken(), missionId);
            if (!result.accepted() && result.tokenInvalid()) {
                log.info("Deregistering notification destination '{}': APNs reports the token is no longer valid ({})",
                        destination.getId(), result.rejectionReason());
                notificationDestinationService.deleteById(destination.getId());
            }
        }
    }
}
