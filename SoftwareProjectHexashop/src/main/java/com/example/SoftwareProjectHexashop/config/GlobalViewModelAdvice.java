package com.example.SoftwareProjectHexashop.config;

import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import com.example.SoftwareProjectHexashop.service.CartService;

import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

@ControllerAdvice
@RequiredArgsConstructor
public class GlobalViewModelAdvice {

    private final CartService cartService;

    @ModelAttribute("cartItemCount")
    public int cartItemCount(HttpSession session) {
        return cartService.getItemCount(session);
    }

    @ModelAttribute("currentUserEmail")
    public String currentUserEmail(Authentication authentication) {
        return authentication != null ? authentication.getName() : null;
    }
}
