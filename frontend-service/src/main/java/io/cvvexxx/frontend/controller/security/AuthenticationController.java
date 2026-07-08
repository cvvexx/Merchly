package io.cvvexxx.frontend.controller.security;


import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller

public class AuthenticationController {

    @GetMapping("/login")
    public String loginPage() {
        return "security/login"; // Возвращает login.html из templates
    }

}
