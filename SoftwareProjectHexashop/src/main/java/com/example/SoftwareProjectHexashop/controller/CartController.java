package com.example.SoftwareProjectHexashop.controller;

import java.util.Map;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.SoftwareProjectHexashop.dto.OrderRequest;
import com.example.SoftwareProjectHexashop.entity.User;
import com.example.SoftwareProjectHexashop.service.CartService;
import com.example.SoftwareProjectHexashop.service.OrderService;
import com.example.SoftwareProjectHexashop.service.UserService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class CartController {

    private final CartService cartService;
    private final OrderService orderService;
    private final UserService userService;

    @GetMapping("/cart")
    public String cartPage(HttpSession session, Model model) {
        model.addAttribute("cartItems", cartService.getCartItems(session));
        model.addAttribute("cartTotal", cartService.getCartTotal(session));
        return "cart";
    }

    @PostMapping(value = "/cart/add", headers = "X-Requested-With=XMLHttpRequest")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> addToCartAjax(@RequestParam Long productId,
            @RequestParam(defaultValue = "1") Integer quantity,
            HttpSession session) {
        try {
            cartService.addItem(session, productId, quantity);
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(Map.of(
                    "message", ex.getMessage(),
                    "cartItemCount", cartService.getItemCount(session)
            ));
        }

        return ResponseEntity.ok(Map.of(
                "message", "Item added to cart.",
                "cartItemCount", cartService.getItemCount(session)
        ));
    }

    @PostMapping("/cart/add")
    public String addToCart(@RequestParam Long productId,
            @RequestParam(defaultValue = "1") Integer quantity,
            HttpSession session,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes) {
        try {
            cartService.addItem(session, productId, quantity);
            redirectAttributes.addFlashAttribute("successMessage", "Item added to cart.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }

        String referer = request.getHeader("Referer");
        return "redirect:" + (referer == null ? "/products" : referer);
    }

    @GetMapping("/cart/add")
    public String addToCartGet(@RequestParam Long productId,
            @RequestParam(defaultValue = "1") Integer quantity,
            HttpSession session,
            HttpServletRequest request,
            RedirectAttributes redirectAttributes) {
        try {
            cartService.addItem(session, productId, quantity);
            redirectAttributes.addFlashAttribute("successMessage", "Item added to cart.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        String referer = request.getHeader("Referer");
        return "redirect:" + (referer == null ? "/products" : referer);
    }

    @PostMapping("/cart/update")
    public String updateCart(@RequestParam Long productId,
            @RequestParam Integer quantity,
            HttpSession session) {
        cartService.updateItem(session, productId, quantity);
        return "redirect:/cart";
    }

    @PostMapping("/cart/remove")
    public String removeFromCart(@RequestParam Long productId, HttpSession session) {
        cartService.removeItem(session, productId);
        return "redirect:/cart";
    }

    @PostMapping("/cart/checkout")
    @PreAuthorize("hasAnyRole('BUYER','SELLER','ADMIN')")
    public String checkout(Authentication authentication,
            HttpSession session,
            RedirectAttributes redirectAttributes) {
        if (cartService.getItemCount(session) == 0) {
            redirectAttributes.addFlashAttribute("errorMessage", "Your cart is empty.");
            return "redirect:/cart";
        }

        User buyer = userService.findByEmail(authentication.getName());
        OrderRequest orderRequest = cartService.toOrderRequest(session);
        orderService.createOrder(orderRequest, buyer);
        cartService.clear(session);

        redirectAttributes.addFlashAttribute("successMessage", "Checkout successful. Order created.");
        return "redirect:/orders";
    }
}
