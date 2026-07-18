package dev.nilswitt.mission_manager.api;

import dev.nilswitt.mission_manager.api.dto.*;
import dev.nilswitt.mission_manager.data.entities.EmbeddableLocation;
import dev.nilswitt.mission_manager.data.entities.LogBookEntry;
import dev.nilswitt.mission_manager.data.entities.Mission;
import dev.nilswitt.mission_manager.data.entities.StoredFile;
import dev.nilswitt.mission_manager.data.entities.User;
import dev.nilswitt.mission_manager.data.services.LogBookEntryService;
import dev.nilswitt.mission_manager.data.services.MissionService;
import dev.nilswitt.mission_manager.data.services.StoredFileService;
import dev.nilswitt.mission_manager.security.PermissionVerifier;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.transaction.Transactional;
import org.springframework.core.io.Resource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriUtils;

import java.net.URLConnection;
import java.nio.charset.StandardCharsets;
import java.util.Set;
import java.util.UUID;

import static dev.nilswitt.mission_manager.data.entities.SecurityGroup.UserRoleScopeEnum.EDIT;

@RestController
@RequestMapping("/api/missions/{missionId}/logbook")
@Tag(name = "Mission Log Book", description = "Log book entries belonging to a mission; an entry cannot exist without its mission and cannot be moved to another one")
public class MissionLogBookApiController {

    private final MissionService missionService;
    private final LogBookEntryService logBookEntryService;
    private final StoredFileService storedFileService;
    private final PermissionVerifier permissionVerifier;

    public MissionLogBookApiController(
            MissionService missionService,
            LogBookEntryService logBookEntryService,
            StoredFileService storedFileService,
            PermissionVerifier permissionVerifier
    ) {
        this.missionService = missionService;
        this.logBookEntryService = logBookEntryService;
        this.storedFileService = storedFileService;
        this.permissionVerifier = permissionVerifier;
    }

    @GetMapping
    @Transactional
    public PageResponse<LogBookEntryResponse> list(
            @AuthenticationPrincipal User currentUser,
            @PathVariable UUID missionId,
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable
    ) {
        Mission mission = findMissionOrThrow(missionId);
        requireAccess(currentUser, mission);

        return PageResponse.from(
                logBookEntryService.findByMission(mission, pageable),
                entry -> LogBookEntryResponse.from(entry, permissionVerifier.getScopes(mission, currentUser))
        );
    }

    @PostMapping
    @Transactional
    public ResponseEntity<?> add(
            @AuthenticationPrincipal User currentUser,
            @PathVariable UUID missionId,
            @RequestBody LogBookEntryRequest request
    ) {
        Mission mission = findMissionOrThrow(missionId);
        requireAccess(currentUser, mission);

        if (request.text() == null || request.text().isBlank()) {
            return ResponseEntity.badRequest().body(new ErrorResponse("Text is required."));
        }

        boolean hasSubmissionId = request.submissionId() != null && !request.submissionId().isBlank();
        if (hasSubmissionId) {
            LogBookEntry existing = logBookEntryService.findBySubmissionId(request.submissionId()).orElse(null);
            if (existing != null) {
                return existing.getMission().getId().equals(mission.getId())
                        ? ResponseEntity.ok(LogBookEntryResponse.from(existing, permissionVerifier.getScopes(mission, currentUser)))
                        : ResponseEntity.status(HttpStatus.CONFLICT)
                            .body(new ErrorResponse("This submission id was already used for a different mission."));
            }
        }

        LogBookEntry entry = new LogBookEntry();
        entry.setMission(mission);
        entry.setAuthor(currentUser.getUsername());
        entry.setSubmissionId(request.submissionId());
        applyDetails(entry, request);

        try {
            logBookEntryService.save(entry);
        } catch (DataIntegrityViolationException ex) {
            if (!hasSubmissionId) {
                throw ex;
            }
            LogBookEntry existing = logBookEntryService
                    .findBySubmissionId(request.submissionId())
                    .orElseThrow(() -> ex);
            return ResponseEntity.ok(LogBookEntryResponse.from(existing, permissionVerifier.getScopes(mission, currentUser)));
        }

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(LogBookEntryResponse.from(entry, permissionVerifier.getScopes(mission, currentUser)));
    }

    @PostMapping("/attachments")
    public ResponseEntity<?> uploadAttachment(
            @AuthenticationPrincipal User currentUser,
            @PathVariable UUID missionId,
            @RequestParam(required = false) String name,
            @RequestParam MultipartFile file,
            @RequestParam(required = false) Double latitude,
            @RequestParam(required = false) Double longitude,
            @RequestParam(required = false) Double height,
            @RequestParam(required = false) String locationName
    ) {
        Mission mission = findMissionOrThrow(missionId);
        requireAccess(currentUser, mission);

        if (file == null || file.isEmpty()) {
            return ResponseEntity.badRequest().body(new ErrorResponse("File is required."));
        }

        EmbeddableLocation location = new EmbeddableLocation(latitude, longitude, height, locationName);
        StoredFile storedFile = storedFileService.store(file, name, location);

        return ResponseEntity.status(HttpStatus.CREATED).body(StoredFileResponse.from(storedFile));
    }

    @GetMapping("/attachments/{fileId}")
    public ResponseEntity<Resource> downloadAttachment(
            @AuthenticationPrincipal User currentUser,
            @PathVariable UUID missionId,
            @PathVariable UUID fileId
    ) {
        Mission mission = findMissionOrThrow(missionId);
        requireAccess(currentUser, mission);

        StoredFile storedFile = storedFileService.findById(fileId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!logBookEntryService.existsByMissionAndAttachment(mission, fileId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        Resource resource = storedFileService.load(storedFile);
        String encodedFilename = UriUtils.encode(storedFile.getOriginalFileName(), StandardCharsets.UTF_8);

        return ResponseEntity.ok()
                .contentType(detectContentType(storedFile.getOriginalFileName()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedFilename)
                .body(resource);
    }

    private MediaType detectContentType(String filename) {
        String guessed = URLConnection.guessContentTypeFromName(filename);
        return guessed != null ? MediaType.parseMediaType(guessed) : MediaType.APPLICATION_OCTET_STREAM;
    }

    private void applyDetails(LogBookEntry entry, LogBookEntryRequest request) {
        entry.setText(request.text());
        entry.setSender(request.sender());
        entry.setRecipient(request.recipient());
        entry.setLocation(request.location() != null ? request.location() : new EmbeddableLocation());
        applyAttachments(entry, request.attachmentIds());
    }

    private void applyAttachments(LogBookEntry entry, Set<UUID> attachmentIds) {
        entry.getAttachments().clear();
        if (attachmentIds != null) {
            entry.getAttachments().addAll(storedFileService.findAllById(attachmentIds));
        }
    }

    private Mission findMissionOrThrow(UUID id) {
        return missionService.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    private void requireAccess(User currentUser, Mission mission) {
        if (!permissionVerifier.hasAccess(currentUser, EDIT, mission)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
    }
}
