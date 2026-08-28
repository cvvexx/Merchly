package io.cvvexxx.frontend.controller.security;

import io.cvvexxx.frontend.client.user.publIc.RestClientUserPublicRestClient;
import io.cvvexxx.frontend.dto.user.CreatedUserDto;
import io.cvvexxx.frontend.dto.user.NewUserDto;
import io.cvvexxx.frontend.exception.BadRequestException;
import io.cvvexxx.frontend.exception.FieldAlreadyExistsException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Controller
@RequiredArgsConstructor
@Slf4j
public class RegistrationController {

    private final RestClientUserPublicRestClient userRestClient;

    @GetMapping("/registration")
    public String getRegistrationPage(
            Model model,
            @RequestParam(name = "target", required = false) String target
    ) {
        model.addAttribute("target", target);
        return "security/registration";
    }

    @PostMapping("/do-register")
    public String registerUser(
            NewUserDto newUserDto,
            MultipartFile userAvatar,
            @RequestParam(name = "target", required = false) String target,
            Model model
    ) {
        try {
            CreatedUserDto createdUserDto = userRestClient.registerUser(newUserDto, userAvatar);
            log.info("Successfully registered user: {}", createdUserDto.username());
            model.addAttribute("target", target);
            return "redirect:/login";
        } catch (BadRequestException exception) {
            model.addAttribute("payload", newUserDto);
            model.addAttribute("errors", exception.getErrors());
            return "security/registration";
        } catch (FieldAlreadyExistsException exception) {
            model.addAttribute("payload", newUserDto);
            model.addAttribute("errors", List.of(exception.getMessage()));
            return "security/registration";
        } catch (Exception e) {
            log.error("Ошибка при регистрации: {}", e.getMessage());
            return "redirect:/registration?error=invalid_data";
        }
    }
}
