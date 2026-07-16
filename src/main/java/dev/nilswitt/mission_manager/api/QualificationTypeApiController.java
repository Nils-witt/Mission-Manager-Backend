package dev.nilswitt.mission_manager.api;

import dev.nilswitt.mission_manager.api.dto.ErrorResponse;
import dev.nilswitt.mission_manager.api.dto.PageResponse;
import dev.nilswitt.mission_manager.api.dto.QualificationTypeRequest;
import dev.nilswitt.mission_manager.api.dto.QualificationTypeResponse;
import dev.nilswitt.mission_manager.data.entities.QualificationType;
import dev.nilswitt.mission_manager.data.entities.SecurityGroup;
import dev.nilswitt.mission_manager.data.entities.User;
import dev.nilswitt.mission_manager.data.services.QualificationTypeService;
import dev.nilswitt.mission_manager.security.PermissionVerifier;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.Set;
import java.util.UUID;

import static dev.nilswitt.mission_manager.data.entities.SecurityGroup.UserRoleScopeEnum.CREATE;
import static dev.nilswitt.mission_manager.data.entities.SecurityGroup.UserRoleScopeEnum.DELETE;
import static dev.nilswitt.mission_manager.data.entities.SecurityGroup.UserRoleScopeEnum.EDIT;
import static dev.nilswitt.mission_manager.data.entities.SecurityGroup.UserRoleScopeEnum.VIEW;
import static dev.nilswitt.mission_manager.data.entities.SecurityGroup.UserRoleTypeEnum.QUALIFICATION;

@RestController
@RequestMapping("/api/qualification-types")
@Tag(name = "Qualification Types", description = "CRUD operations for qualification types")
public class QualificationTypeApiController {

    private final QualificationTypeService qualificationTypeService;
    private final PermissionVerifier permissionVerifier;

    public QualificationTypeApiController(QualificationTypeService qualificationTypeService, PermissionVerifier permissionVerifier) {
        this.qualificationTypeService = qualificationTypeService;
        this.permissionVerifier = permissionVerifier;
    }

    @GetMapping
    public PageResponse<QualificationTypeResponse> list(
        @AuthenticationPrincipal User currentUser,
        @RequestParam(required = false) String name,
        @PageableDefault(size = 20, sort = "createdAt") Pageable pageable
    ) {
        requireScope(currentUser, VIEW);
        return PageResponse.from(
            qualificationTypeService.findAll(name, pageable),
            type -> QualificationTypeResponse.from(type, permissions(currentUser))
        );
    }

    @GetMapping("/{id}")
    public QualificationTypeResponse get(@AuthenticationPrincipal User currentUser, @PathVariable UUID id) {
        requireScope(currentUser, VIEW);
        return QualificationTypeResponse.from(findTypeOrThrow(id), permissions(currentUser));
    }

    @PostMapping
    public ResponseEntity<?> create(
        @AuthenticationPrincipal User currentUser,
        @RequestBody QualificationTypeRequest request
    ) {
        requireScope(currentUser, CREATE);

        QualificationType type = new QualificationType(request.name());
        try {
            qualificationTypeService.save(type);
        } catch (DataIntegrityViolationException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse("A qualification type with this name already exists."));
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(QualificationTypeResponse.from(type, permissions(currentUser)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(
        @AuthenticationPrincipal User currentUser,
        @PathVariable UUID id,
        @RequestBody QualificationTypeRequest request
    ) {
        requireScope(currentUser, EDIT);
        QualificationType target = findTypeOrThrow(id);
        target.setName(request.name());

        try {
            qualificationTypeService.save(target);
        } catch (DataIntegrityViolationException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse("A qualification type with this name already exists."));
        }

        return ResponseEntity.ok(QualificationTypeResponse.from(target, permissions(currentUser)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@AuthenticationPrincipal User currentUser, @PathVariable UUID id) {
        requireScope(currentUser, DELETE);
        findTypeOrThrow(id);

        try {
            qualificationTypeService.deleteById(id);
        } catch (DataIntegrityViolationException ex) {
            return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse("Cannot delete a type that is still used by a qualification."));
        }

        return ResponseEntity.noContent().build();
    }

    private QualificationType findTypeOrThrow(UUID id) {
        return qualificationTypeService.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
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
