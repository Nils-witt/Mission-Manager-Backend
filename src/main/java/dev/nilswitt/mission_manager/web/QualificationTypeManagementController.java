package dev.nilswitt.mission_manager.web;

import dev.nilswitt.mission_manager.data.entities.QualificationType;
import dev.nilswitt.mission_manager.data.entities.SecurityGroup;
import dev.nilswitt.mission_manager.data.entities.User;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

import static dev.nilswitt.mission_manager.data.entities.SecurityGroup.UserRoleScopeEnum.CREATE;
import static dev.nilswitt.mission_manager.data.entities.SecurityGroup.UserRoleScopeEnum.DELETE;
import static dev.nilswitt.mission_manager.data.entities.SecurityGroup.UserRoleScopeEnum.EDIT;
import static dev.nilswitt.mission_manager.data.entities.SecurityGroup.UserRoleScopeEnum.VIEW;
import static dev.nilswitt.mission_manager.data.entities.SecurityGroup.UserRoleTypeEnum.QUALIFICATION;

@Controller
@RequestMapping("/qualification-types")
public class QualificationTypeManagementController {

    private final QualificationTypeService qualificationTypeService;

    public QualificationTypeManagementController(QualificationTypeService qualificationTypeService) {
        this.qualificationTypeService = qualificationTypeService;
    }

    @GetMapping
    public String list(
        @AuthenticationPrincipal User currentUser,
        @RequestParam(required = false) String error,
        Model model
    ) {
        requireScope(currentUser, VIEW);
        model.addAttribute("username", currentUser.getUsername());
        model.addAttribute("types", qualificationTypeService.findAll());
        model.addAttribute("canCreate", PermissionVerifier.hasAnyScope(currentUser, QUALIFICATION, CREATE));
        model.addAttribute("error", error);
        return "qualification-types/list";
    }

    @GetMapping("/new")
    public String newForm(@AuthenticationPrincipal User currentUser, Model model) {
        requireScope(currentUser, CREATE);
        model.addAttribute("username", currentUser.getUsername());
        model.addAttribute("form", new QualificationTypeFormModel());
        model.addAttribute("isNew", true);
        return "qualification-types/form";
    }

    @PostMapping
    public String create(
        @AuthenticationPrincipal User currentUser,
        @ModelAttribute("form") QualificationTypeFormModel form,
        Model model
    ) {
        requireScope(currentUser, CREATE);

        QualificationType type = new QualificationType(form.getName());

        try {
            qualificationTypeService.save(type);
        } catch (DataIntegrityViolationException ex) {
            return reRenderForm(model, currentUser, form, true, null, "A qualification type with this name already exists.");
        }

        return "redirect:/qualification-types";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@AuthenticationPrincipal User currentUser, @PathVariable UUID id, Model model) {
        requireScope(currentUser, EDIT);
        QualificationType target = findTypeOrThrow(id);

        QualificationTypeFormModel form = new QualificationTypeFormModel();
        form.setName(target.getName());

        model.addAttribute("username", currentUser.getUsername());
        model.addAttribute("form", form);
        model.addAttribute("isNew", false);
        model.addAttribute("typeId", id);
        return "qualification-types/form";
    }

    @PostMapping("/{id}")
    public String update(
        @AuthenticationPrincipal User currentUser,
        @PathVariable UUID id,
        @ModelAttribute("form") QualificationTypeFormModel form,
        Model model
    ) {
        requireScope(currentUser, EDIT);
        QualificationType target = findTypeOrThrow(id);

        target.setName(form.getName());

        try {
            qualificationTypeService.save(target);
        } catch (DataIntegrityViolationException ex) {
            return reRenderForm(model, currentUser, form, false, id, "A qualification type with this name already exists.");
        }

        return "redirect:/qualification-types";
    }

    @PostMapping("/{id}/delete")
    public String delete(@AuthenticationPrincipal User currentUser, @PathVariable UUID id) {
        requireScope(currentUser, DELETE);
        findTypeOrThrow(id);

        try {
            qualificationTypeService.deleteById(id);
        } catch (DataIntegrityViolationException ex) {
            return "redirect:/qualification-types?error=Cannot delete a type that is still used by a qualification.";
        }

        return "redirect:/qualification-types";
    }

    private QualificationType findTypeOrThrow(UUID id) {
        return qualificationTypeService.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    private void requireScope(User currentUser, SecurityGroup.UserRoleScopeEnum scope) {
        if (!PermissionVerifier.hasAnyScope(currentUser, QUALIFICATION, scope)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
    }

    private String reRenderForm(
        Model model,
        User currentUser,
        QualificationTypeFormModel form,
        boolean isNew,
        UUID typeId,
        String error
    ) {
        model.addAttribute("username", currentUser.getUsername());
        model.addAttribute("form", form);
        model.addAttribute("isNew", isNew);
        model.addAttribute("typeId", typeId);
        model.addAttribute("error", error);
        return "qualification-types/form";
    }
}
