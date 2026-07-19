package dev.nilswitt.mission_manager.data.services;

import com.eatthepath.pushy.apns.*;
import com.eatthepath.pushy.apns.auth.ApnsSigningKey;
import com.eatthepath.pushy.apns.util.ApnsPayloadBuilder;
import com.eatthepath.pushy.apns.util.SimpleApnsPayloadBuilder;
import com.eatthepath.pushy.apns.util.SimpleApnsPushNotification;
import com.eatthepath.pushy.apns.util.TokenUtil;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.time.Instant;
import java.util.Date;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Thin wrapper around a Pushy {@link ApnsClient}. The client is only built when APNs is enabled
 * and fully configured; otherwise every send is a logged no-op so the rest of the app can call
 * this unconditionally without checking whether push is set up.
 */
@Slf4j
@Service
public class ApnsService {

    private final ApnsClient apnsClient;
    private final String topic;

    public ApnsService(
            @Value("${application.apns.enabled:false}") boolean enabled,
            @Value("${application.apns.production:false}") boolean production,
            @Value("${application.apns.signing-key-path:}") String signingKeyPath,
            @Value("${application.apns.key-id:}") String keyId,
            @Value("${application.apns.team-id:}") String teamId,
            @Value("${application.apns.topic:}") String topic
    ) {
        this.topic = topic;
        this.apnsClient = enabled ? buildClient(production, signingKeyPath, keyId, teamId) : null;
    }

    private ApnsClient buildClient(boolean production, String signingKeyPath, String keyId, String teamId) {
        if (signingKeyPath.isBlank() || keyId.isBlank() || teamId.isBlank()) {
            log.warn("APNs is enabled but signing-key-path/key-id/team-id are not fully configured; push notifications will be skipped.");
            return null;
        }
        try {
            return new ApnsClientBuilder()
                    .setApnsServer(production ? ApnsClientBuilder.PRODUCTION_APNS_HOST : ApnsClientBuilder.DEVELOPMENT_APNS_HOST)
                    .setSigningKey(ApnsSigningKey.loadFromPkcs8File(new File(signingKeyPath), teamId, keyId))
                    .build();
        } catch (Exception e) {
            log.error("Failed to initialize APNs client; push notifications will be skipped.", e);
            return null;
        }
    }

    public record PushResult(boolean accepted, boolean tokenInvalid, String rejectionReason) {
    }

    public PushResult send(String deviceToken, String title, String body) {
        if (apnsClient == null) {
            return new PushResult(false, false, "APNs is not configured");
        }

        ApnsPayloadBuilder payloadBuilder = new SimpleApnsPayloadBuilder();
        payloadBuilder.setAlertTitle(title);
        payloadBuilder.setAlertBody(body);
        payloadBuilder.setSound("default");

        String token = TokenUtil.sanitizeTokenString(deviceToken);
        ApnsPushNotification notification = new SimpleApnsPushNotification(token, topic, payloadBuilder.build());

        try {
            PushNotificationResponse<ApnsPushNotification> response = apnsClient.sendNotification(notification).get(10, TimeUnit.SECONDS);

            if (response.isAccepted()) {
                return new PushResult(true, false, null);
            }

            String rejectionReason = response.getRejectionReason().orElse("unknown");
            boolean tokenInvalid = response.getTokenInvalidationTimestamp().isPresent();
            log.warn("APNs rejected push to token '{}': {}", token, rejectionReason);
            return new PushResult(false, tokenInvalid, rejectionReason);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Interrupted while sending APNs push to token '{}'", token, e);
            return new PushResult(false, false, "interrupted");
        } catch (ExecutionException | TimeoutException e) {
            log.error("Failed to send APNs push to token '{}'", token, e);
            return new PushResult(false, false, "send failed");
        }
    }

    public PushResult sendBGPushUpdatedMission(String deviceToken, String missionId) {
        if (apnsClient == null) {
            return new PushResult(false, false, "APNs is not configured");
        }

        String payload = "{ \"aps\": { \"content-available\": 1 }, \"missionId\": \"" + missionId + "\" }";
        String token = TokenUtil.sanitizeTokenString(deviceToken);

        ApnsPushNotification notification = new SimpleApnsPushNotification(token, topic, payload, Instant.ofEpochMilli(new Date().getTime() + 3600000), DeliveryPriority.CONSERVE_POWER, PushType.BACKGROUND);


        try {
            PushNotificationResponse<ApnsPushNotification> response = apnsClient.sendNotification(notification).get(10, TimeUnit.SECONDS);

            if (response.isAccepted()) {
                return new PushResult(true, false, null);
            }

            String rejectionReason = response.getRejectionReason().orElse("unknown");
            boolean tokenInvalid = response.getTokenInvalidationTimestamp().isPresent();
            log.warn("APNs rejected push to token '{}': {}", token, rejectionReason);
            return new PushResult(false, tokenInvalid, rejectionReason);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("Interrupted while sending APNs push to token '{}'", token, e);
            return new PushResult(false, false, "interrupted");
        } catch (ExecutionException | TimeoutException e) {
            log.error("Failed to send APNs push to token '{}'", token, e);
            return new PushResult(false, false, "send failed");
        }
    }

    @PreDestroy
    public void shutdown() {
        if (apnsClient == null) {
            return;
        }
        try {
            apnsClient.close().get(5, TimeUnit.SECONDS);
        } catch (Exception e) {
            log.warn("Failed to cleanly close APNs client", e);
        }
    }
}
