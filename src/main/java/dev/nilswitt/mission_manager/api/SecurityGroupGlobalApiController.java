package dev.nilswitt.mission_manager.api;

import dev.nilswitt.mission_manager.data.entities.SecurityGroup;
import dev.nilswitt.mission_manager.data.entities.SecurityRole;
import dev.nilswitt.mission_manager.data.entities.User;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/security-groups")
@Tag(name = "Security Groups", description = "Global States for SecurityGroups")
public class SecurityGroupGlobalApiController {

    public SecurityGroupGlobalApiController() {
    }

    @GetMapping("/roles")
    public List<SecurityRole> listAvailableRoles(@AuthenticationPrincipal User currentUser) {
        return SecurityGroup.availableRoles();
    }

}
