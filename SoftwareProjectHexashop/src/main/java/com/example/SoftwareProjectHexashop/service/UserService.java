package com.example.SoftwareProjectHexashop.service;

import com.example.SoftwareProjectHexashop.dto.RegisterRequest;
import com.example.SoftwareProjectHexashop.dto.UserResponse;
import com.example.SoftwareProjectHexashop.entity.Role;
import com.example.SoftwareProjectHexashop.entity.RoleName;
import com.example.SoftwareProjectHexashop.entity.User;
import com.example.SoftwareProjectHexashop.exception.ResourceNotFoundException;
import com.example.SoftwareProjectHexashop.repository.RoleRepository;
import com.example.SoftwareProjectHexashop.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.HashSet;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public User register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new IllegalArgumentException("Email is already registered");
        }

        RoleName targetRole = request.getRole() == null ? RoleName.BUYER : request.getRole();
        if (targetRole == RoleName.ADMIN) {
            throw new IllegalArgumentException("Admin accounts cannot be self-registered");
        }

        Role role = roleRepository.findByName(targetRole)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found: " + targetRole));

        boolean enabled = targetRole != RoleName.SELLER;
        return createUser(request.getFullName(), request.getEmail(), request.getPassword(), role, enabled);
    }

    @Transactional(readOnly = true)
    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + email));
    }

    @Transactional(readOnly = true)
    public List<UserResponse> getAllUsers() {
        return userRepository.findAll()
                .stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<UserResponse> getUsersByRole(RoleName roleName) {
        return userRepository.findAll().stream()
                .filter(user -> user.getRoles() != null && user.getRoles().stream().anyMatch(role -> role.getName() == roleName))
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<UserResponse> getPendingSellerApprovals() {
        return userRepository.findAll().stream()
                .filter(user -> !user.isEnabled())
                .filter(user -> user.getRoles() != null && user.getRoles().stream().anyMatch(role -> role.getName() == RoleName.SELLER))
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public User createSellerByAdmin(String fullName, String email, String rawPassword) {
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email is already registered");
        }

        Role sellerRole = roleRepository.findByName(RoleName.SELLER)
                .orElseThrow(() -> new ResourceNotFoundException("Role not found: SELLER"));
        return createUser(fullName, email, rawPassword, sellerRole, true);
    }

    @Transactional
    public void approveSeller(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        boolean seller = user.getRoles() != null && user.getRoles().stream().anyMatch(role -> role.getName() == RoleName.SELLER);
        if (!seller) {
            throw new IllegalArgumentException("User is not a seller account");
        }

        user.setEnabled(true);
        userRepository.save(user);
    }

    @Transactional
    public void removeSellerRole(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        Set<Role> roles = user.getRoles() == null ? new HashSet<>() : new HashSet<>(user.getRoles());
        roles.removeIf(role -> role.getName() == RoleName.SELLER);

        if (roles.stream().noneMatch(role -> role.getName() == RoleName.ADMIN)
                && roles.stream().noneMatch(role -> role.getName() == RoleName.BUYER)) {
            Role buyerRole = roleRepository.findByName(RoleName.BUYER)
                    .orElseThrow(() -> new ResourceNotFoundException("Role not found: BUYER"));
            roles.add(buyerRole);
        }

        user.setRoles(roles);
        userRepository.save(user);
    }

    private UserResponse toResponse(User user) {
        return UserResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .roles(user.getRoles().stream().map(r -> r.getName().name()).collect(Collectors.toSet()))
                .build();
    }

    private User createUser(String fullName, String email, String rawPassword, Role role, boolean enabled) {
        User user = User.builder()
                .fullName(fullName)
                .email(email)
                .password(passwordEncoder.encode(rawPassword))
                .roles(Set.of(role))
                .enabled(enabled)
                .build();
        return userRepository.save(user);
    }
}
