package dev.nilswitt.mission_manager.web;

import lombok.Data;

import java.util.UUID;

@Data
public class EmailComposeFormModel {

    private String recipientType;
    private UUID recipientId;
    private String subject;
    private String body;
}
