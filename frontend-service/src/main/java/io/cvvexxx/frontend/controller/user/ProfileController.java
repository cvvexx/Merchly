package io.cvvexxx.frontend.controller.user;


import io.cvvexxx.frontend.client.user.publIc.RestClientUserPublicRestClient;
import io.cvvexxx.frontend.dto.UserInfoDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.OAuth2AuthorizedClient;
import org.springframework.security.oauth2.client.annotation.RegisteredOAuth2AuthorizedClient;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final RestClientUserPublicRestClient restClient;

    @GetMapping
    public String userProfilePage(Authentication authentication, Model model) {
        UserInfoDto userInfo = restClient.getUserInfo();
        model.addAttribute("user", userInfo);
        return "user/profile";
    }
}

