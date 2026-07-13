package dev.nilswitt.mission_manager.web;

import dev.nilswitt.mission_manager.data.entities.Qualification;
import dev.nilswitt.mission_manager.data.entities.SecurityGroup;
import dev.nilswitt.mission_manager.data.entities.User;
import dev.nilswitt.mission_manager.data.services.QualificationService;
import dev.nilswitt.mission_manager.data.services.QualificationTypeService;
import dev.nilswitt.mission_manager.security.PermissionVerifier;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import static dev.nilswitt.mission_manager.data.entities.SecurityGroup.UserRoleScopeEnum.CREATE;
import static dev.nilswitt.mission_manager.data.entities.SecurityGroup.UserRoleScopeEnum.DELETE;
import static dev.nilswitt.mission_manager.data.entities.SecurityGroup.UserRoleScopeEnum.EDIT;
import static dev.nilswitt.mission_manager.data.entities.SecurityGroup.UserRoleScopeEnum.VIEW;
import static dev.nilswitt.mission_manager.data.entities.SecurityGroup.UserRoleTypeEnum.QUALIFICATION;

@Controller
@RequestMapping("/qualifications")
public class QualificationManagementController {

    private final QualificationService qualificationService;
    private final QualificationTypeService qualificationTypeService;

    public QualificationManagementController(
        QualificationService qualificationService,
        QualificationTypeService qualificationTypeService
    ) {
        this.qualificationService = qualificationService;
        this.qualificationTypeService = qualificationTypeService;
    }

    @GetMapping
    public String list(@AuthenticationPrincipal User currentUser, Model model) {
        requireScope(currentUser, VIEW);
        model.addAttribute("username", currentUser.getUsername());
        model.addAttribute("qualifications", qualificationService.findAll());
        model.addAttribute("canCreate", PermissionVerifier.hasAnyScope(currentUser, QUALIFICATION, CREATE));
        return "qualifications/list";
    }

    @GetMapping("/new")
    public String newForm(@AuthenticationPrincipal User currentUser, Model model) {
        requireScope(currentUser, CREATE);
        model.addAttribute("username", currentUser.getUsername());
        model.addAttribute("form", new QualificationFormModel());
        model.addAttribute("availableQualifications", qualificationService.findAll());
        model.addAttribute("types", qualificationTypeService.findAll());
        model.addAttribute("isNew", true);
        return "qualifications/form";
    }

    @PostMapping
    public String create(
        @AuthenticationPrincipal User currentUser,
        @ModelAttribute("form") QualificationFormModel form,
        Model model
    ) {
        requireScope(currentUser, CREATE);

        if (form.getTypeId() == null) {
            return reRenderForm(model, currentUser, form, true, null, "Type is required.");
        }

        Qualification qualification = new Qualification(
            form.getName(),
            qualificationTypeService.findById(form.getTypeId()).orElse(null)
        );
        applyIncludedQualifications(qualification, form.getIncludedQualificationIds());

        try {
            qualificationService.save(qualification);
        } catch (DataIntegrityViolationException ex) {
            return reRenderForm(model, currentUser, form, true, null, "Unable to save this qualification.");
        }

        return "redirect:/qualifications";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@AuthenticationPrincipal User currentUser, @PathVariable UUID id, Model model) {
        requireScope(currentUser, EDIT);
        Qualification target = findQualificationOrThrow(id);

        QualificationFormModel form = new QualificationFormModel();
        form.setName(target.getName());
        form.setTypeId(target.getType().getId());
        form.setIncludedQualificationIds(
            target.getIncludedQualifications().stream().map(Qualification::getId).collect(Collectors.toSet())
        );

        model.addAttribute("username", currentUser.getUsername());
        model.addAttribute("form", form);
        model.addAttribute("availableQualifications", otherQualifications(id));
        model.addAttribute("types", qualificationTypeService.findAll());
        model.addAttribute("isNew", false);
        model.addAttribute("qualificationId", id);
        return "qualifications/form";
    }

    @PostMapping("/{id}")
    public String update(
        @AuthenticationPrincipal User currentUser,
        @PathVariable UUID id,
        @ModelAttribute("form") QualificationFormModel form,
        Model model
    ) {
        requireScope(currentUser, EDIT);
        Qualification target = findQualificationOrThrow(id);

        if (form.getTypeId() == null) {
            return reRenderForm(model, currentUser, form, false, id, "Type is required.");
        }

        target.setName(form.getName());
        target.setType(qualificationTypeService.findById(form.getTypeId()).orElse(null));
        applyIncludedQualifications(target, form.getIncludedQualificationIds());

        try {
            qualificationService.save(target);
        } catch (DataIntegrityViolationException ex) {
            return reRenderForm(model, currentUser, form, false, id, "Unable to save this qualification.");
        }

        return "redirect:/qualifications";
    }

    @PostMapping("/{id}/delete")
    public String delete(@AuthenticationPrincipal User currentUser, @PathVariable UUID id) {
        requireScope(currentUser, DELETE);
        findQualificationOrThrow(id);
        qualificationService.deleteById(id);
        return "redirect:/qualifications";
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

    private List<Qualification> otherQualifications(UUID excludedId) {
        return qualificationService.findAll().stream().filter(q -> !q.getId().equals(excludedId)).toList();
    }

    private Qualification findQualificationOrThrow(UUID id) {
        return qualificationService.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    private void requireScope(User currentUser, SecurityGroup.UserRoleScopeEnum scope) {
        if (!PermissionVerifier.hasAnyScope(currentUser, QUALIFICATION, scope)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
    }

    private String reRenderForm(
        Model model,
        User currentUser,
        QualificationFormModel form,
        boolean isNew,
        UUID qualificationId,
        String error
    ) {
        model.addAttribute("username", currentUser.getUsername());
        model.addAttribute("form", form);
        model.addAttribute(
            "availableQualifications",
            qualificationId == null ? qualificationService.findAll() : otherQualifications(qualificationId)
        );
        model.addAttribute("types", qualificationTypeService.findAll());
        model.addAttribute("isNew", isNew);
        model.addAttribute("qualificationId", qualificationId);
        model.addAttribute("error", error);
        return "qualifications/form";
    }
}
