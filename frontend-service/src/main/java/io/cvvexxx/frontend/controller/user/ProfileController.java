package io.cvvexxx.frontend.controller.user;


import io.cvvexxx.frontend.client.user.publIc.RestClientUserPublicRestClient;
import io.cvvexxx.frontend.dto.UserInfoDto;
import lombok.RequiredArgsConstructor;
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
    public String userProfilePage(Model model) {
        UserInfoDto userInfo = restClient.getUserInfo();
        model.addAttribute("user", userInfo);
        return "user/profile";
    }
}

