package com.example.SoftwareProjectHexashop.controller;

import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.SoftwareProjectHexashop.dto.OrderRequest;
import com.example.SoftwareProjectHexashop.dto.OrderResponse;
import com.example.SoftwareProjectHexashop.entity.User;
import com.example.SoftwareProjectHexashop.service.OrderService;
import com.example.SoftwareProjectHexashop.service.UserService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderApiController {

    private final OrderService orderService;
    private final UserService userService;

    @PostMapping
    @PreAuthorize("hasAnyRole('BUYER','ADMIN')")
    public ResponseEntity<OrderResponse> createOrder(@Valid @RequestBody OrderRequest request,
                                                     Authentication authentication) {
        User buyer = userService.findByEmail(authentication.getName());
        return ResponseEntity.status(HttpStatus.CREATED).body(orderService.createOrder(request, buyer));
    }

    @GetMapping("/my")
    @PreAuthorize("hasAnyRole('BUYER','ADMIN')")
    public List<OrderResponse> getMyOrders(Authentication authentication) {
        User buyer = userService.findByEmail(authentication.getName());
        return orderService.getOrdersForBuyer(buyer);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('BUYER','ADMIN')")
    public OrderResponse getOrderById(@PathVariable Long id) {
        return orderService.getOrderById(id);
    }
}
