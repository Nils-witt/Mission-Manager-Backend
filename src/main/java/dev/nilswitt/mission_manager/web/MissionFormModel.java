package dev.nilswitt.mission_manager.web;

import lombok.Data;

import java.util.UUID;

@Data
public class MissionFormModel {

    private String name;
    private UUID tenantId;
}
