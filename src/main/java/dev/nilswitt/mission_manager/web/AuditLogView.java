package dev.nilswitt.mission_manager.web;

import dev.nilswitt.mission_manager.data.entities.AuditLog;
import lombok.Getter;

import java.util.List;

/**
 * Presentation wrapper that merges an {@link AuditLog}'s old/new value JSON into a single
 * per-field diff list, so the template can render "field: old -> new" without doing any
 * JSON handling itself.
 */
@Getter
public class AuditLogView {

    private final AuditLog log;
    private final List<FieldChange> changes;

    public AuditLogView(AuditLog log, List<FieldChange> changes) {
        this.log = log;
        this.changes = changes;
    }

    public record FieldChange(String field, String oldValue, String newValue) {}
}
