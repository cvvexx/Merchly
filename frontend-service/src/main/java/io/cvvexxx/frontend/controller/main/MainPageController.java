package io.cvvexxx.frontend.controller.main;


import io.cvvexxx.frontend.dto.UserDto;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class MainPageController {

    public static final String DEFAULT_USERNAME = "User";

    @GetMapping
    public String getMainPage(
            @AuthenticationPrincipal UserDto userDto,
            Model model
    ) {
        if (userDto != null) {
            model.addAttribute("username", userDto.username());
        } else {
            model.addAttribute("username", DEFAULT_USERNAME);
        }

        return "catalogue/main/main_page";
    }

}
