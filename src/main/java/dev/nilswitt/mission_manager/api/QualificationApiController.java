package dev.nilswitt.mission_manager.api;

import dev.nilswitt.mission_manager.api.dto.ErrorResponse;
import dev.nilswitt.mission_manager.api.dto.QualificationRequest;
import dev.nilswitt.mission_manager.api.dto.QualificationResponse;
import dev.nilswitt.mission_manager.data.entities.Qualification;
import dev.nilswitt.mission_manager.data.entities.SecurityGroup;
import dev.nilswitt.mission_manager.data.entities.User;
import dev.nilswitt.mission_manager.data.services.QualificationService;
import dev.nilswitt.mission_manager.data.services.QualificationTypeService;
import dev.nilswitt.mission_manager.security.PermissionVerifier;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static dev.nilswitt.mission_manager.data.entities.SecurityGroup.UserRoleScopeEnum.CREATE;
import static dev.nilswitt.mission_manager.data.entities.SecurityGroup.UserRoleScopeEnum.DELETE;
import static dev.nilswitt.mission_manager.data.entities.SecurityGroup.UserRoleScopeEnum.EDIT;
import static dev.nilswitt.mission_manager.data.entities.SecurityGroup.UserRoleScopeEnum.VIEW;
import static dev.nilswitt.mission_manager.data.entities.SecurityGroup.UserRoleTypeEnum.QUALIFICATION;

@RestController
@RequestMapping("/api/qualifications")
@Tag(name = "Qualifications", description = "CRUD operations for qualifications")
public class QualificationApiController {

    private final QualificationService qualificationService;
    private final QualificationTypeService qualificationTypeService;
    private final PermissionVerifier permissionVerifier;

    public QualificationApiController(
        QualificationService qualificationService,
        QualificationTypeService qualificationTypeService,
        PermissionVerifier permissionVerifier
    ) {
        this.qualificationService = qualificationService;
        this.qualificationTypeService = qualificationTypeService;
        this.permissionVerifier = permissionVerifier;
    }

    @GetMapping
    public List<QualificationResponse> list(@AuthenticationPrincipal User currentUser) {
        requireScope(currentUser, VIEW);
        return qualificationService.findAll().stream()
            .map(qualification -> QualificationResponse.from(qualification, permissions(currentUser)))
            .toList();
    }

    @GetMapping("/{id}")
    public QualificationResponse get(@AuthenticationPrincipal User currentUser, @PathVariable UUID id) {
        requireScope(currentUser, VIEW);
        return QualificationResponse.from(findQualificationOrThrow(id), permissions(currentUser));
    }

    @PostMapping
    public ResponseEntity<?> create(@AuthenticationPrincipal User currentUser, @RequestBody QualificationRequest request) {
        requireScope(currentUser, CREATE);

        if (request.typeId() == null) {
            return ResponseEntity.badRequest().body(new ErrorResponse("Type is required."));
        }

        Qualification qualification = new Qualification(
            request.name(),
            qualificationTypeService.findById(request.typeId()).orElse(null)
        );
        applyIncludedQualifications(qualification, request.includedQualificationIds());

        try {
            qualificationService.save(qualification);
        } catch (DataIntegrityViolationException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse("Unable to save this qualification."));
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(QualificationResponse.from(qualification, permissions(currentUser)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(
        @AuthenticationPrincipal User currentUser,
        @PathVariable UUID id,
        @RequestBody QualificationRequest request
    ) {
        requireScope(currentUser, EDIT);
        Qualification target = findQualificationOrThrow(id);

        if (request.typeId() == null) {
            return ResponseEntity.badRequest().body(new ErrorResponse("Type is required."));
        }

        target.setName(request.name());
        target.setType(qualificationTypeService.findById(request.typeId()).orElse(null));
        applyIncludedQualifications(target, request.includedQualificationIds());

        try {
            qualificationService.save(target);
        } catch (DataIntegrityViolationException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse("Unable to save this qualification."));
        }

        return ResponseEntity.ok(QualificationResponse.from(target, permissions(currentUser)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@AuthenticationPrincipal User currentUser, @PathVariable UUID id) {
        requireScope(currentUser, DELETE);
        findQualificationOrThrow(id);
        qualificationService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    private void applyIncludedQualifications(Qualification qualification, Set<UUID> includedIds) {
        qualification.getIncludedQualifications().clear();
        if (includedIds == null) {
            return;
        }
        includedIds
            .stream()
            .filter(includedId -> !includedId.equals(qualification.getId()))
            .forEach(includedId -> qualificationService.findById(includedId).ifPresent(qualification.getIncludedQualifications()::add));
    }

    private Qualification findQualificationOrThrow(UUID id) {
        return qualificationService.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    private void requireScope(User currentUser, SecurityGroup.UserRoleScopeEnum scope) {
        if (!PermissionVerifier.hasAnyScope(currentUser, QUALIFICATION, scope)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
    }

    private Set<SecurityGroup.UserRoleScopeEnum> permissions(User currentUser) {
        return permissionVerifier.getScopes(QUALIFICATION, currentUser);
    }
}
