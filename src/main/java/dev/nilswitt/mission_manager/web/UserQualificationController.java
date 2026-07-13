package dev.nilswitt.mission_manager.web;

import dev.nilswitt.mission_manager.data.entities.Qualification;
import dev.nilswitt.mission_manager.data.entities.SecurityGroup;
import dev.nilswitt.mission_manager.data.entities.User;
import dev.nilswitt.mission_manager.data.entities.UserQualification;
import dev.nilswitt.mission_manager.data.services.QualificationService;
import dev.nilswitt.mission_manager.data.services.UserQualificationService;
import dev.nilswitt.mission_manager.data.services.UserService;
import dev.nilswitt.mission_manager.security.PermissionVerifier;
import org.springframework.core.io.Resource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriUtils;

import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.UUID;

import static dev.nilswitt.mission_manager.data.entities.SecurityGroup.UserRoleScopeEnum.EDIT;
import static dev.nilswitt.mission_manager.data.entities.SecurityGroup.UserRoleScopeEnum.VIEW;

@Controller
@RequestMapping("/users/{userId}/qualifications")
public class UserQualificationController {

    private final UserService userService;
    private final QualificationService qualificationService;
    private final UserQualificationService userQualificationService;
    private final PermissionVerifier permissionVerifier;

    public UserQualificationController(
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

    @PostMapping
    public String add(
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
            return redirectWithError(userId, "Qualification is required.");
        }
        if (since == null) {
            return redirectWithError(userId, "Since date is required.");
        }
        if (expiry != null && expiry.isBefore(since)) {
            return redirectWithError(userId, "Expiry cannot be before the since date.");
        }

        Qualification qualification = qualificationService.findById(qualificationId).orElse(null);
        if (qualification == null) {
            return redirectWithError(userId, "Qualification not found.");
        }

        UserQualification userQualification = new UserQualification(targetUser, qualification, since, expiry);
        userQualification.setActive(active);
        userQualificationService.save(userQualification, certificate);

        return "redirect:/users/" + userId + "/edit";
    }

    @PostMapping("/{id}/toggle-active")
    public String toggleActive(
        @AuthenticationPrincipal User currentUser,
        @PathVariable UUID userId,
        @PathVariable UUID id
    ) {
        User targetUser = findUserOrThrow(userId);
        requireAccess(currentUser, EDIT, targetUser);
        UserQualification userQualification = findAssignmentOrThrow(id, targetUser);

        userQualification.setActive(!userQualification.isActive());
        userQualificationService.save(userQualification, null);

        return "redirect:/users/" + userId + "/edit";
    }

    @PostMapping("/{id}/delete")
    public String delete(
        @AuthenticationPrincipal User currentUser,
        @PathVariable UUID userId,
        @PathVariable UUID id
    ) {
        User targetUser = findUserOrThrow(userId);
        requireAccess(currentUser, EDIT, targetUser);
        findAssignmentOrThrow(id, targetUser);

        userQualificationService.deleteById(id);
        return "redirect:/users/" + userId + "/edit";
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

    private String redirectWithError(UUID userId, String message) {
        return "redirect:/users/" + userId + "/edit?qualificationError=" + UriUtils.encode(message, StandardCharsets.UTF_8);
    }
}
