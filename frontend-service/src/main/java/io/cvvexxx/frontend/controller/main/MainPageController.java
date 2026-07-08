package io.cvvexxx.frontend.controller.main;


import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class MainPageController {

    @GetMapping
    public String getMainPage() {
        return "catalogue/main/main_page";
    }

}
