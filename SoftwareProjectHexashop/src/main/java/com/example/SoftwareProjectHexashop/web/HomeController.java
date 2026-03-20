package com.example.SoftwareProjectHexashop.web;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/")
    public String home() {
        // Redirect root path to the default Spring Security login page.
        return "redirect:/login";
    }
}
