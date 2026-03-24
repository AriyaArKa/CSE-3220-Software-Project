package com.example.SoftwareProjectHexashop.service;

import com.example.SoftwareProjectHexashop.dto.RegisterRequest;
import com.example.SoftwareProjectHexashop.entity.Role;
import com.example.SoftwareProjectHexashop.entity.RoleName;
import com.example.SoftwareProjectHexashop.entity.User;
import com.example.SoftwareProjectHexashop.exception.ResourceNotFoundException;
import com.example.SoftwareProjectHexashop.repository.RoleRepository;
import com.example.SoftwareProjectHexashop.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    @Captor
    private ArgumentCaptor<User> userCaptor;

    private Role buyerRole;
    private Role sellerRole;

    @BeforeEach
    void setUp() {
        buyerRole = Role.builder().id(1L).name(RoleName.BUYER).build();
        sellerRole = Role.builder().id(2L).name(RoleName.SELLER).build();
    }

    @Test
    void registerBuyerSuccessfully() {
        RegisterRequest request = request("Buyer One", "buyer@example.com", "plain-pass", RoleName.BUYER);

        when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(roleRepository.findByName(RoleName.BUYER)).thenReturn(Optional.of(buyerRole));
        when(passwordEncoder.encode(request.getPassword())).thenReturn("encoded-pass");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User created = userService.register(request);

        assertEquals("buyer@example.com", created.getEmail());
        assertTrue(created.isEnabled());
        assertTrue(created.getRoles().stream().anyMatch(r -> r.getName() == RoleName.BUYER));
    }

    @Test
    void rejectDuplicateEmailRegistration() {
        RegisterRequest request = request("Buyer Two", "duplicate@example.com", "secret", RoleName.BUYER);
        when(userRepository.existsByEmail(request.getEmail())).thenReturn(true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class, () -> userService.register(request));

        assertEquals("Email is already registered", ex.getMessage());
    }

    @Test
    void encodePasswordOnRegistration() {
        RegisterRequest request = request("Buyer Three", "buyer3@example.com", "raw-password", RoleName.BUYER);

        when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(roleRepository.findByName(RoleName.BUYER)).thenReturn(Optional.of(buyerRole));
        when(passwordEncoder.encode("raw-password")).thenReturn("encoded-password");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        userService.register(request);

        verify(userRepository).save(userCaptor.capture());
        assertEquals("encoded-password", userCaptor.getValue().getPassword());
    }

    @Test
    void registerSellerInPendingState() {
        RegisterRequest request = request("Seller One", "seller@example.com", "seller-pass", RoleName.SELLER);

        when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(roleRepository.findByName(RoleName.SELLER)).thenReturn(Optional.of(sellerRole));
        when(passwordEncoder.encode(request.getPassword())).thenReturn("encoded-seller-pass");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        User created = userService.register(request);

        assertFalse(created.isEnabled());
        assertTrue(created.getRoles().stream().anyMatch(r -> r.getName() == RoleName.SELLER));
    }

    @Test
    void approvePendingSeller() {
        User pendingSeller = User.builder()
                .id(99L)
                .fullName("Pending Seller")
                .email("pending@example.com")
                .password("encoded")
                .enabled(false)
                .roles(Set.of(sellerRole))
                .build();

        when(userRepository.findById(99L)).thenReturn(Optional.of(pendingSeller));

        userService.approveSeller(99L);

        assertTrue(pendingSeller.isEnabled());
        verify(userRepository).save(pendingSeller);
    }

    @Test
    void removeSellerRoleAddsBuyerWhenNoOtherRoleExists() {
        User sellerOnlyUser = User.builder()
                .id(5L)
                .fullName("Seller Only")
                .email("seller-only@example.com")
                .password("encoded")
                .enabled(true)
                .roles(Set.of(sellerRole))
                .build();

        when(userRepository.findById(5L)).thenReturn(Optional.of(sellerOnlyUser));
        when(roleRepository.findByName(RoleName.BUYER)).thenReturn(Optional.of(buyerRole));

        userService.removeSellerRole(5L);

        verify(userRepository).save(userCaptor.capture());
        User saved = userCaptor.getValue();
        assertTrue(saved.getRoles().stream().anyMatch(r -> r.getName() == RoleName.BUYER));
        assertFalse(saved.getRoles().stream().anyMatch(r -> r.getName() == RoleName.SELLER));
    }

    @Test
    void registerFailsWhenRoleMissing() {
        RegisterRequest request = request("Buyer Four", "buyer4@example.com", "pass", RoleName.BUYER);

        when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(roleRepository.findByName(RoleName.BUYER)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> userService.register(request));
    }

    private RegisterRequest request(String name, String email, String password, RoleName roleName) {
        RegisterRequest request = new RegisterRequest();
        request.setFullName(name);
        request.setEmail(email);
        request.setPassword(password);
        request.setRole(roleName);
        return request;
    }
}
