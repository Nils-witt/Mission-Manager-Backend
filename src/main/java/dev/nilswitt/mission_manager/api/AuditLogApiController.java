package dev.nilswitt.mission_manager.api;

import dev.nilswitt.mission_manager.api.dto.AuditLogResponse;
import dev.nilswitt.mission_manager.api.dto.PageResponse;
import dev.nilswitt.mission_manager.data.entities.AuditLog;
import dev.nilswitt.mission_manager.data.entities.SecurityGroup;
import dev.nilswitt.mission_manager.data.entities.User;
import dev.nilswitt.mission_manager.data.repositories.AuditLogRepository;
import dev.nilswitt.mission_manager.security.PermissionVerifier;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
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

@RestController
@RequestMapping("/api/audit-logs")
@Tag(name = "Audit Logs", description = "Read-only audit trail of entity changes")
public class AuditLogApiController {

    private final AuditLogRepository auditLogRepository;
    private final ObjectMapper objectMapper;
    private final PermissionVerifier permissionVerifier;

    public AuditLogApiController(
        AuditLogRepository auditLogRepository,
        ObjectMapper objectMapper,
        PermissionVerifier permissionVerifier
    ) {
        this.auditLogRepository = auditLogRepository;
        this.objectMapper = objectMapper;
        this.permissionVerifier = permissionVerifier;
    }

    @GetMapping
    public PageResponse<AuditLogResponse> list(
        @AuthenticationPrincipal User currentUser,
        @RequestParam(required = false) String entityName,
        @RequestParam(required = false) UUID entityId,
        @PageableDefault(size = 20, sort = "changedAt", direction = Sort.Direction.DESC) Pageable pageable
    ) {
        if (!PermissionVerifier.hasAnyScope(currentUser, AUDITLOG, VIEW)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        Page<AuditLog> logs;
        if (entityName != null && !entityName.isBlank() && entityId != null) {
            logs = auditLogRepository.findByEntityNameAndEntityId(entityName, entityId, pageable);
        } else if (entityName != null && !entityName.isBlank()) {
            logs = auditLogRepository.findByEntityName(entityName, pageable);
        } else {
            logs = auditLogRepository.findAll(pageable);
        }

        Set<SecurityGroup.UserRoleScopeEnum> permissions = permissionVerifier.getScopes(AUDITLOG, currentUser);
        return PageResponse.from(logs, log -> toResponse(log, permissions));
    }

    private AuditLogResponse toResponse(AuditLog log, Set<SecurityGroup.UserRoleScopeEnum> permissions) {
        Map<String, Object> oldValues = parseJson(log.getOldValues());
        Map<String, Object> newValues = parseJson(log.getNewValues());

        Set<String> fields = new LinkedHashSet<>();
        fields.addAll(oldValues.keySet());
        fields.addAll(newValues.keySet());

        List<AuditLogResponse.FieldChange> changes = fields
            .stream()
            .map(field -> new AuditLogResponse.FieldChange(field, stringify(oldValues.get(field)), stringify(newValues.get(field))))
            .toList();

        return AuditLogResponse.from(log, changes, permissions);
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
