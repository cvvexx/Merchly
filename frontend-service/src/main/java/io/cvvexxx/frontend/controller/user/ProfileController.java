package io.cvvexxx.frontend.controller.user;


import io.cvvexxx.frontend.client.user.publIc.RestClientUserPublicRestClient;
import io.cvvexxx.frontend.dto.UserInfoDto;
import io.cvvexxx.frontend.utils.ImageUrlFormatter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/profile")
@RequiredArgsConstructor
@Slf4j
public class ProfileController {

    private final RestClientUserPublicRestClient restClient;
    private final ImageUrlFormatter imageUrlFormatter;

    @GetMapping
    public String userProfilePage(Model model) {
        UserInfoDto userInfo = restClient.getUserInfo();
        log.info("userInfo: {}", userInfo);
        model.addAttribute("user", userInfo);
        model.addAttribute("userAvatar", imageUrlFormatter.getUserAvatarUrl(userInfo.userAvatarUrl()));
        return "user/profile";
    }   
}

