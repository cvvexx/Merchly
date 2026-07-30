package io.cvvexxx.frontend.controller.main;


import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@Slf4j
public class MainPageController {

    public static final String DEFAULT_USERNAME = "User";

    @GetMapping
    public String getMainPage(
            @AuthenticationPrincipal String username,
            Model model
    ) {
        if (username.equals("anonymousUser")) {
            model.addAttribute("username", DEFAULT_USERNAME);
        } else {
            model.addAttribute("username", username);
        }
        return "catalogue/main/main_page";
    }

}
