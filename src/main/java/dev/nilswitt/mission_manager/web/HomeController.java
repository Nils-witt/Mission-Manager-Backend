package dev.nilswitt.mission_manager.web;

import dev.nilswitt.mission_manager.data.entities.User;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/")
    public String home(@AuthenticationPrincipal User user, Model model) {
        model.addAttribute("username", user.getUsername());
        return "home";
    }
}
