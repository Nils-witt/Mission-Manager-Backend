package dev.nilswitt.mission_manager.web;

import lombok.Data;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Data
public class QualificationFormModel {

    private String name;
    private UUID typeId;
    private Set<UUID> includedQualificationIds = new HashSet<>();
}
