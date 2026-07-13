package dev.nilswitt.mission_manager.web;

import dev.nilswitt.mission_manager.data.entities.AuditLog;
import dev.nilswitt.mission_manager.data.entities.User;
import dev.nilswitt.mission_manager.data.repositories.AuditLogRepository;
import dev.nilswitt.mission_manager.security.PermissionVerifier;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

import static dev.nilswitt.mission_manager.data.entities.SecurityGroup.UserRoleScopeEnum.VIEW;
import static dev.nilswitt.mission_manager.data.entities.SecurityGroup.UserRoleTypeEnum.AUDITLOG;

@Controller
@RequestMapping("/audit-logs")
public class AuditLogController {

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;

    public AuditLogController(AuditLogRepository auditLogRepository, ObjectMapper objectMapper) {
        this.auditLogRepository = auditLogRepository;
        this.objectMapper = objectMapper;
    }

    @GetMapping
    public String list(
        @AuthenticationPrincipal User currentUser,
        @RequestParam(required = false) String entityName,
        @RequestParam(required = false) UUID entityId,
        Model model
    ) {
        if (!PermissionVerifier.hasAnyScope(currentUser, AUDITLOG, VIEW)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        List<AuditLog> logs;
        if (entityName != null && !entityName.isBlank() && entityId != null) {
            logs = auditLogRepository.findTop200ByEntityNameAndEntityIdOrderByChangedAtDesc(entityName, entityId);
        } else if (entityName != null && !entityName.isBlank()) {
            logs = auditLogRepository.findTop200ByEntityNameOrderByChangedAtDesc(entityName);
        } else {
            logs = auditLogRepository.findTop200ByOrderByChangedAtDesc();
        }

        model.addAttribute("username", currentUser.getUsername());
        model.addAttribute("logs", logs.stream().map(this::toView).toList());
        model.addAttribute("entityName", entityName);
        model.addAttribute("entityId", entityId);
        return "audit-logs/list";
    }

    private AuditLogView toView(AuditLog log) {
        Map<String, Object> oldValues = parseJson(log.getOldValues());
        Map<String, Object> newValues = parseJson(log.getNewValues());

        Set<String> fields = new LinkedHashSet<>();
        fields.addAll(oldValues.keySet());
        fields.addAll(newValues.keySet());

        List<AuditLogView.FieldChange> changes = fields
            .stream()
            .map(field -> new AuditLogView.FieldChange(field, stringify(oldValues.get(field)), stringify(newValues.get(field))))
            .toList();

        return new AuditLogView(log, changes);
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> parseJson(String json) {
        if (json == null || json.isBlank()) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, LinkedHashMap.class);
        } catch (JacksonException e) {
            return Map.of();
        }
    }

    private String stringify(Object value) {
        return value == null ? null : String.valueOf(value);
    }
}
