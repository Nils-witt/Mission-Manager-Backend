package dev.nilswitt.mission_manager.api;

import dev.nilswitt.mission_manager.api.dto.ErrorResponse;
import dev.nilswitt.mission_manager.api.dto.PageResponse;
import dev.nilswitt.mission_manager.api.dto.UserQualificationResponse;
import dev.nilswitt.mission_manager.data.entities.Qualification;
import dev.nilswitt.mission_manager.data.entities.SecurityGroup;
import dev.nilswitt.mission_manager.data.entities.User;
import dev.nilswitt.mission_manager.data.entities.UserQualification;
import dev.nilswitt.mission_manager.data.services.QualificationService;
import dev.nilswitt.mission_manager.data.services.UserQualificationService;
import dev.nilswitt.mission_manager.data.services.UserService;
import dev.nilswitt.mission_manager.security.PermissionVerifier;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.web.PageableDefault;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.UUID;

import static dev.nilswitt.mission_manager.data.entities.SecurityGroup.UserRoleScopeEnum.EDIT;
import static dev.nilswitt.mission_manager.data.entities.SecurityGroup.UserRoleScopeEnum.VIEW;

@RestController
@RequestMapping("/api/users/{userId}/qualifications")
@Tag(name = "User Qualifications", description = "Qualifications assigned to a user, with certificate uploads")
public class UserQualificationApiController {

    private final UserService userService;
    private final QualificationService qualificationService;
    private final UserQualificationService userQualificationService;
    private final PermissionVerifier permissionVerifier;

    public UserQualificationApiController(
        UserService userService,
        QualificationService qualificationService,
        UserQualificationService userQualificationService,
        PermissionVerifier permissionVerifier
    ) {
        this.userService = userService;
        this.qualificationService = qualificationService;
        this.userQualificationService = userQualificationService;
        this.permissionVerifier = permissionVerifier;
    }

    @GetMapping
    public PageResponse<UserQualificationResponse> list(
        @AuthenticationPrincipal User currentUser,
        @PathVariable UUID userId,
        @RequestParam(required = false) UUID qualificationId,
        @RequestParam(required = false) Boolean active,
        @PageableDefault(size = 20, sort = "createdAt") Pageable pageable
    ) {
        User targetUser = findUserOrThrow(userId);
        requireAccess(currentUser, VIEW, targetUser);

        Specification<UserQualification> spec = Specifications.allOf(
            (root, query, cb) -> cb.equal(root.get("user").get("id"), userId),
            qualificationEquals(qualificationId),
            activeEquals(active)
        );

        return PageResponse.from(
            userQualificationService.findAll(spec, pageable),
            uq -> UserQualificationResponse.from(uq, permissionVerifier.getScopes(targetUser, currentUser))
        );
    }

    private static Specification<UserQualification> qualificationEquals(UUID qualificationId) {
        if (qualificationId == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("qualification").get("id"), qualificationId);
    }

    private static Specification<UserQualification> activeEquals(Boolean active) {
        if (active == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get("isActive"), active);
    }

    @PostMapping
    public ResponseEntity<?> add(
        @AuthenticationPrincipal User currentUser,
        @PathVariable UUID userId,
        @RequestParam(required = false) UUID qualificationId,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate since,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate expiry,
        @RequestParam(defaultValue = "false") boolean active,
        @RequestParam(required = false) MultipartFile certificate
    ) {
        User targetUser = findUserOrThrow(userId);
        requireAccess(currentUser, EDIT, targetUser);

        if (qualificationId == null) {
            return ResponseEntity.badRequest().body(new ErrorResponse("Qualification is required."));
        }
        if (since == null) {
            return ResponseEntity.badRequest().body(new ErrorResponse("Since date is required."));
        }
        if (expiry != null && expiry.isBefore(since)) {
            return ResponseEntity.badRequest().body(new ErrorResponse("Expiry cannot be before the since date."));
        }

        Qualification qualification = qualificationService.findById(qualificationId).orElse(null);
        if (qualification == null) {
            return ResponseEntity.badRequest().body(new ErrorResponse("Qualification not found."));
        }

        UserQualification userQualification = new UserQualification(targetUser, qualification, since, expiry);
        userQualification.setActive(active);
        userQualificationService.save(userQualification, certificate);

        return ResponseEntity.status(HttpStatus.CREATED)
            .body(UserQualificationResponse.from(userQualification, permissionVerifier.getScopes(targetUser, currentUser)));
    }

    @PostMapping("/{id}/toggle-active")
    public UserQualificationResponse toggleActive(
        @AuthenticationPrincipal User currentUser,
        @PathVariable UUID userId,
        @PathVariable UUID id
    ) {
        User targetUser = findUserOrThrow(userId);
        requireAccess(currentUser, EDIT, targetUser);
        UserQualification userQualification = findAssignmentOrThrow(id, targetUser);

        userQualification.setActive(!userQualification.isActive());
        userQualificationService.save(userQualification, null);

        return UserQualificationResponse.from(userQualification, permissionVerifier.getScopes(targetUser, currentUser));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
        @AuthenticationPrincipal User currentUser,
        @PathVariable UUID userId,
        @PathVariable UUID id
    ) {
        User targetUser = findUserOrThrow(userId);
        requireAccess(currentUser, EDIT, targetUser);
        findAssignmentOrThrow(id, targetUser);

        userQualificationService.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/certificate")
    public ResponseEntity<Resource> downloadCertificate(
        @AuthenticationPrincipal User currentUser,
        @PathVariable UUID userId,
        @PathVariable UUID id
    ) {
        User targetUser = findUserOrThrow(userId);
        requireAccess(currentUser, VIEW, targetUser);
        UserQualification userQualification = findAssignmentOrThrow(id, targetUser);

        if (!userQualification.hasCertificate()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        Resource resource = userQualificationService.loadCertificate(userQualification);
        String filename = userQualification.getCertificateOriginalFilename() != null
            ? userQualification.getCertificateOriginalFilename()
            : "certificate";
        String encodedFilename = UriUtils.encode(filename, StandardCharsets.UTF_8);

        MediaType contentType = userQualification.getCertificateContentType() != null
            ? MediaType.parseMediaType(userQualification.getCertificateContentType())
            : MediaType.APPLICATION_OCTET_STREAM;

        return ResponseEntity.ok()
            .contentType(contentType)
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedFilename)
            .body(resource);
    }

    private UserQualification findAssignmentOrThrow(UUID id, User targetUser) {
        UserQualification userQualification = userQualificationService
            .findById(id)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!userQualification.getUser().getId().equals(targetUser.getId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return userQualification;
    }

    private User findUserOrThrow(UUID id) {
        return userService.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    private void requireAccess(User currentUser, SecurityGroup.UserRoleScopeEnum scope, User target) {
        if (!permissionVerifier.hasAccess(currentUser, scope, target)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
    }
}
