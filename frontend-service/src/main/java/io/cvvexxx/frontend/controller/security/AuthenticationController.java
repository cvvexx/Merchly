package io.cvvexxx.frontend.controller.security;


import io.cvvexxx.frontend.client.user.RestClientUserRestClient;
import io.cvvexxx.frontend.controller.security.payload.UserLoginPayload;
import io.cvvexxx.frontend.dto.UserDto;
import io.cvvexxx.frontend.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
@RequiredArgsConstructor
public class AuthenticationController {

    private final RestClientUserRestClient restClient;

    @GetMapping("/login")
    public String loginPage() {
        return "security/login";
    }

    @PostMapping("/login")
    public String loginUser(
        UserLoginPayload payload
    ) {
        UserDto userDto = restClient.checkUserAuth(payload.username(), payload.password());

        return "redirect:/catalogue/main/main_page";
    }

}
