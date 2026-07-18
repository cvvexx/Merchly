package io.cvvexxx.frontend.controller.user;


import io.cvvexxx.frontend.client.user.RestClientUserRestClient;
import io.cvvexxx.frontend.dto.UserInfoDto;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final RestClientUserRestClient restClient;

    @GetMapping
    public String userProfilePage(Authentication authentication, Model model) {
        UserInfoDto userInfo = restClient.getUserInfo((String) authentication.getCredentials());
        model.addAttribute("user", userInfo);
        return "user/profile";
    }
}

