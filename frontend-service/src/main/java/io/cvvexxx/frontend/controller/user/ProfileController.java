package io.cvvexxx.frontend.controller.user;


import io.cvvexxx.frontend.dto.UserDto;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/profile")
public class ProfileController {

    @GetMapping
    public String userProfilePage(@AuthenticationPrincipal UserDto currentUser, Model model) {
        model.addAttribute("user", currentUser);
        return "user/profile";
    }
}
