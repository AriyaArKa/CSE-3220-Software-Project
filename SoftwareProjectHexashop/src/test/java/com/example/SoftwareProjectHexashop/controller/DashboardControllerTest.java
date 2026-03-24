package com.example.SoftwareProjectHexashop.controller;

import java.util.Arrays;
import java.util.Collection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;

import com.example.SoftwareProjectHexashop.service.OrderService;
import com.example.SoftwareProjectHexashop.service.ProductService;
import com.example.SoftwareProjectHexashop.service.UserService;

@ExtendWith(MockitoExtension.class)
class DashboardControllerTest {

    @Mock
    private UserService userService;

    @Mock
    private ProductService productService;

    @Mock
    private OrderService orderService;

    @Test
    void roleAwareDashboardRedirectionWorksForAdmin() {
        DashboardController controller = new DashboardController(userService, productService, orderService);
        Authentication authentication = authenticationWithRoles("ROLE_ADMIN");

        String view = controller.dashboard(authentication);

        assertEquals("redirect:/admin/dashboard", view);
    }

    @Test
    void roleAwareDashboardRedirectionWorksForSeller() {
        DashboardController controller = new DashboardController(userService, productService, orderService);
        Authentication authentication = authenticationWithRoles("ROLE_SELLER");

        String view = controller.dashboard(authentication);

        assertEquals("redirect:/seller/dashboard", view);
    }

    @Test
    void roleAwareDashboardRedirectionDefaultsToBuyer() {
        DashboardController controller = new DashboardController(userService, productService, orderService);
        Authentication authentication = authenticationWithRoles("ROLE_BUYER");

        String view = controller.dashboard(authentication);

        assertEquals("redirect:/buyer/dashboard", view);
    }

    private Authentication authenticationWithRoles(String... roles) {
        Authentication authentication = mock(Authentication.class);
        Collection<? extends GrantedAuthority> authorities = Arrays.stream(roles)
                .map(SimpleGrantedAuthority::new)
                .toList();
        doReturn(authorities).when(authentication).getAuthorities();
        return authentication;
    }
}
