package com.example.SoftwareProjectHexashop.controller;

import com.example.SoftwareProjectHexashop.dto.OrderResponse;
import com.example.SoftwareProjectHexashop.dto.UserResponse;
import com.example.SoftwareProjectHexashop.service.OrderService;
import com.example.SoftwareProjectHexashop.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminApiController {

    private final UserService userService;
    private final OrderService orderService;

    @GetMapping("/users")
    public List<UserResponse> users() {
        return userService.getAllUsers();
    }

    @GetMapping("/orders")
    public List<OrderResponse> orders() {
        return orderService.getAllOrders();
    }
}
