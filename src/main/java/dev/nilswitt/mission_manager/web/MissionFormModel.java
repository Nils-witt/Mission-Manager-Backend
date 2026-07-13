package dev.nilswitt.mission_manager.web;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Data
public class MissionFormModel {

    private String name;
    private UUID tenantId;

    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime startTime;

    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm")
    private LocalDateTime endTime;

    private Double latitude;
    private Double longitude;
    private String streetAddress;
    private Set<UUID> availableUserIds = new HashSet<>();
}
