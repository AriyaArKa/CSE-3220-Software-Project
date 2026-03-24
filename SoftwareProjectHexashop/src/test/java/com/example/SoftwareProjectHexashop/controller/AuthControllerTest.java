package com.example.SoftwareProjectHexashop.controller;

import com.example.SoftwareProjectHexashop.dto.RegisterRequest;
import com.example.SoftwareProjectHexashop.entity.RoleName;
import com.example.SoftwareProjectHexashop.entity.User;
import com.example.SoftwareProjectHexashop.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private UserService userService;

    @InjectMocks
    private AuthController authController;

    @Test
    void loginPageReturnsLoginView() {
        assertEquals("auth/login", authController.loginPage());
    }

    @Test
    void registerPageAddsRegisterRequestToModel() {
        Model model = new ExtendedModelMap();

        String view = authController.registerPage(model);

        assertEquals("auth/register", view);
        assertTrue(model.containsAttribute("registerRequest"));
    }

    @Test
    void registerReturnsFormWhenValidationErrorsExist() {
        RegisterRequest request = new RegisterRequest();
        BindingResult bindingResult = mock(BindingResult.class);
        Model model = new ExtendedModelMap();
        when(bindingResult.hasErrors()).thenReturn(true);

        String view = authController.register(request, bindingResult, model);

        assertEquals("auth/register", view);
    }

    @Test
    void registerSellerRedirectsToPendingApproval() {
        RegisterRequest request = new RegisterRequest();
        request.setRole(RoleName.SELLER);
        request.setEmail("seller@example.com");

        User seller = User.builder().email("seller@example.com").enabled(false).build();
        BindingResult bindingResult = mock(BindingResult.class);
        Model model = new ExtendedModelMap();

        when(bindingResult.hasErrors()).thenReturn(false);
        when(userService.register(request)).thenReturn(seller);

        String view = authController.register(request, bindingResult, model);

        assertEquals("redirect:/login?pendingApproval", view);
    }

    @Test
    void registerBuyerRedirectsToRegistered() {
        RegisterRequest request = new RegisterRequest();
        request.setRole(RoleName.BUYER);

        User buyer = User.builder().enabled(true).build();
        BindingResult bindingResult = mock(BindingResult.class);
        Model model = new ExtendedModelMap();

        when(bindingResult.hasErrors()).thenReturn(false);
        when(userService.register(request)).thenReturn(buyer);

        String view = authController.register(request, bindingResult, model);

        assertEquals("redirect:/login?registered", view);
    }

    @Test
    void registerReturnsFormWithErrorMessageOnFailure() {
        RegisterRequest request = new RegisterRequest();
        BindingResult bindingResult = mock(BindingResult.class);
        Model model = new ExtendedModelMap();

        when(bindingResult.hasErrors()).thenReturn(false);
        when(userService.register(request)).thenThrow(new IllegalArgumentException("Email is already registered"));

        String view = authController.register(request, bindingResult, model);

        assertEquals("auth/register", view);
        assertEquals("Email is already registered", model.getAttribute("errorMessage"));
    }
}
