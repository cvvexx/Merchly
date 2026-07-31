package io.cvvexxx.frontend.controller.user;

import io.cvvexxx.frontend.client.user.publIc.UserPublicRestClient;
import io.cvvexxx.frontend.dto.user.UserProfilePublicDto;
import io.cvvexxx.frontend.utils.ImageUrlFormatter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/users")
@RequiredArgsConstructor
@Slf4j
public class PublicProfileController {

    private final UserPublicRestClient userPublicRestClient;
    private final ImageUrlFormatter imageUrlFormatter;

    @GetMapping("/{username}")
    public String showPublicProfile(
            @PathVariable("username") String username,
            Model model,
            @AuthenticationPrincipal String authUsername
    ) {
        if (authUsername.equalsIgnoreCase(username)) {
            return "redirect:/profile";
        }

        UserProfilePublicDto profile = userPublicRestClient.getUserProfile(username);
        model.addAttribute("profile", profile);
        model.addAttribute("userAvatarUrl", imageUrlFormatter.getUserAvatarUrl(profile.userAvatarUrl()));
        return "user/public-profile";
    }
}