package io.cvvexxx.frontend.controller.user;


import io.cvvexxx.frontend.client.user.publIc.RestClientUserPublicRestClient;
import io.cvvexxx.frontend.dto.user.UpdateUserDto;
import io.cvvexxx.frontend.dto.user.UserInfoDto;
import io.cvvexxx.frontend.exception.BadRequestException;
import io.cvvexxx.frontend.exception.FieldAlreadyExistsException;
import io.cvvexxx.frontend.utils.ImageUrlFormatter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Controller
@RequestMapping("/profile")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final RestClientUserPublicRestClient restClient;
    private final ImageUrlFormatter imageUrlFormatter;

    @ModelAttribute("user")
    public UserInfoDto user() {
        return restClient.getUserInfo();
    }

    @GetMapping
    public String userProfilePage(@ModelAttribute("user") UserInfoDto userInfo, Model model) {
        model.addAttribute("user", userInfo);
        model.addAttribute("userAvatar", imageUrlFormatter.getUserAvatarUrl(userInfo.userAvatarUrl()));
        return "user/profile";
    }

    @GetMapping("/edit")
    public String editProfilePage(@ModelAttribute("user") UserInfoDto userInfo, Model model) {
        model.addAttribute("user", userInfo);
        model.addAttribute("userAvatar", imageUrlFormatter.getUserAvatarUrl(userInfo.userAvatarUrl()));
        return "user/edit";
    }

    @PostMapping("/edit")
    public String editUserProfile(
            @ModelAttribute("user") UserInfoDto userInfo,
            MultipartFile userAvatar,
            UpdateUserDto updateUserDto,
            Model model
    ) {
        try {
            restClient.updateUserInfo(updateUserDto, userAvatar);

            return "redirect:/profile";
        } catch (BadRequestException exception) {
            model.addAttribute("userAvatar", imageUrlFormatter.getUserAvatarUrl(userInfo.userAvatarUrl()));
            model.addAttribute("payload", updateUserDto);
            model.addAttribute("errors", exception.getErrors());
            return "user/edit";
        } catch (FieldAlreadyExistsException exception) {
            model.addAttribute("userAvatar", imageUrlFormatter.getUserAvatarUrl(userInfo.userAvatarUrl()));
            model.addAttribute("payload", updateUserDto);
            model.addAttribute("errors", List.of(exception.getMessage()));
            return "security/registration";
        }
    }

    @PostMapping("/admin")
    public ResponseEntity<Void> getAdminRole() {
        restClient.getAdminRole();
        return ResponseEntity.noContent().build();
    }
}

