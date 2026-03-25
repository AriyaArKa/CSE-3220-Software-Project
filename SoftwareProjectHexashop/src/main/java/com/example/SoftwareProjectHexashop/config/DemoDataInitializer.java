package com.example.SoftwareProjectHexashop.config;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import com.example.SoftwareProjectHexashop.entity.Product;
import com.example.SoftwareProjectHexashop.entity.Role;
import com.example.SoftwareProjectHexashop.entity.RoleName;
import com.example.SoftwareProjectHexashop.entity.User;
import com.example.SoftwareProjectHexashop.repository.ProductRepository;
import com.example.SoftwareProjectHexashop.repository.RoleRepository;
import com.example.SoftwareProjectHexashop.repository.UserRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class DemoDataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final ProductRepository productRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.seed-demo-data:true}")
    private boolean seedDemoData;

    @Override
    public void run(String... args) {
        if (!seedDemoData) {
            return;
        }

        Role adminRole = getOrCreateRole(RoleName.ADMIN);
        Role sellerRole = getOrCreateRole(RoleName.SELLER);
        Role buyerRole = getOrCreateRole(RoleName.BUYER);

        createOrUpdateUser("admin@hexashop.com", "Admin User", "admin123", Set.of(adminRole));
        User seller = createOrUpdateUser("seller@hexashop.com", "Demo Seller", "seller123", Set.of(sellerRole));
        createOrUpdateUser("buyer@hexashop.com", "Demo Buyer", "buyer123", Set.of(buyerRole));

        if (productRepository.count() == 0) {
            seedProducts(seller);
        }
    }

    private Role getOrCreateRole(RoleName roleName) {
        return roleRepository.findByName(roleName)
                .orElseGet(() -> roleRepository.save(Role.builder().name(roleName).build()));
    }

        private User createOrUpdateUser(String email, String fullName, String rawPassword, Set<Role> roles) {
                return userRepository.findByEmail(email)
                                .map(existing -> {
                                        boolean changed = false;

                                        if (!existing.isEnabled()) {
                                                existing.setEnabled(true);
                                                changed = true;
                                        }

                                        if (existing.getRoles() == null || existing.getRoles().isEmpty() || !existing.getRoles().equals(roles)) {
                                                existing.setRoles(new HashSet<>(roles));
                                                changed = true;
                                        }

                                        if (existing.getPassword() == null || !passwordEncoder.matches(rawPassword, existing.getPassword())) {
                                                existing.setPassword(passwordEncoder.encode(rawPassword));
                                                changed = true;
                                        }

                                        if (existing.getFullName() == null || existing.getFullName().isBlank()) {
                                                existing.setFullName(fullName);
                                                changed = true;
                                        }

                                        return changed ? userRepository.save(existing) : existing;
                                })
                                .orElseGet(() -> userRepository.save(User.builder()
                                                .email(email)
                                                .fullName(fullName)
                                                .password(passwordEncoder.encode(rawPassword))
                                                .enabled(true)
                                                .roles(new HashSet<>(roles))
                                                .build()));
    }

    private void seedProducts(User seller) {
        List<Product> products = List.of(
                Product.builder()
                        .name("Classic Spring Jacket")
                        .description("Lightweight spring jacket for daily wear.")
                        .price(new BigDecimal("120.00"))
                        .stock(30)
                        .imageUrl("/images/men-01.jpg")
                        .seller(seller)
                        .build(),
                Product.builder()
                        .name("Urban Air Shoes")
                        .description("Comfort-focused sneakers for city walking.")
                        .price(new BigDecimal("90.00"))
                        .stock(50)
                        .imageUrl("/images/men-02.jpg")
                        .seller(seller)
                        .build(),
                Product.builder()
                        .name("Elegant Green Jacket")
                        .description("Smart-casual jacket for work and weekends.")
                        .price(new BigDecimal("75.00"))
                        .stock(22)
                        .imageUrl("/images/women-01.jpg")
                        .seller(seller)
                        .build(),
                Product.builder()
                        .name("Classic Dress")
                        .description("A timeless dress for events and evening outings.")
                        .price(new BigDecimal("45.00"))
                        .stock(45)
                        .imageUrl("/images/women-02.jpg")
                        .seller(seller)
                        .build(),
                Product.builder()
                        .name("Kids School Collection")
                        .description("Durable and comfortable school essentials for kids.")
                        .price(new BigDecimal("80.00"))
                        .stock(35)
                        .imageUrl("/images/kid-01.jpg")
                        .seller(seller)
                        .build(),
                Product.builder()
                        .name("Summer Cap")
                        .description("Breathable cap designed for sunny outdoor days.")
                        .price(new BigDecimal("12.00"))
                        .stock(80)
                        .imageUrl("/images/kid-02.jpg")
                        .seller(seller)
                        .build()
        );

        productRepository.saveAll(products);
    }
}