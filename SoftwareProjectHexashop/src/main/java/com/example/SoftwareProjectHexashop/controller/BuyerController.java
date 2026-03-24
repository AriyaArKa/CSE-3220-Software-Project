package com.example.SoftwareProjectHexashop.controller;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.SoftwareProjectHexashop.dto.OrderItemRequest;
import com.example.SoftwareProjectHexashop.dto.OrderRequest;
import com.example.SoftwareProjectHexashop.entity.User;
import com.example.SoftwareProjectHexashop.service.OrderService;
import com.example.SoftwareProjectHexashop.service.UserService;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class BuyerController {

    private final OrderService orderService;
    private final UserService userService;

    @PostMapping("/buyer/orders")
    @PreAuthorize("hasAnyRole('BUYER','ADMIN')")
    public String placeOrder(@RequestParam Long productId,
                             @RequestParam Integer quantity,
                             Authentication authentication,
                             RedirectAttributes redirectAttributes) {
        User buyer = userService.findByEmail(authentication.getName());

        OrderItemRequest itemRequest = new OrderItemRequest();
        itemRequest.setProductId(productId);
        itemRequest.setQuantity(quantity);

        OrderRequest orderRequest = new OrderRequest();
        orderRequest.setItems(List.of(itemRequest));
        orderService.createOrder(orderRequest, buyer);

        redirectAttributes.addFlashAttribute("successMessage", "Order placed successfully.");
        return "redirect:/orders";
    }
}
