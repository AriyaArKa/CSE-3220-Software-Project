package com.example.SoftwareProjectHexashop.controller;

import com.example.SoftwareProjectHexashop.dto.OrderResponse;
import com.example.SoftwareProjectHexashop.dto.UserResponse;
import com.example.SoftwareProjectHexashop.service.OrderService;
import com.example.SoftwareProjectHexashop.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminApiControllerTest {

    @Mock
    private UserService userService;

    @Mock
    private OrderService orderService;

    @InjectMocks
    private AdminApiController adminApiController;

    @Test
    void usersReturnsAllUsersFromService() {
        List<UserResponse> expected = List.of(
                UserResponse.builder().id(1L).fullName("Admin").email("admin@example.com").roles(Set.of("ADMIN")).build(),
                UserResponse.builder().id(2L).fullName("Buyer").email("buyer@example.com").roles(Set.of("BUYER")).build()
        );
        when(userService.getAllUsers()).thenReturn(expected);

        List<UserResponse> actual = adminApiController.users();

        assertEquals(expected, actual);
    }

    @Test
    void ordersReturnsAllOrdersFromService() {
        List<OrderResponse> expected = List.of(
                OrderResponse.builder().id(10L).build(),
                OrderResponse.builder().id(11L).build()
        );
        when(orderService.getAllOrders()).thenReturn(expected);

        List<OrderResponse> actual = adminApiController.orders();

        assertEquals(expected, actual);
    }
}
