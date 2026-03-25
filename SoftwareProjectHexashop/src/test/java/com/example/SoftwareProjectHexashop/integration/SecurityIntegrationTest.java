package com.example.SoftwareProjectHexashop.integration;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import com.example.SoftwareProjectHexashop.entity.Role;
import com.example.SoftwareProjectHexashop.entity.RoleName;
import com.example.SoftwareProjectHexashop.entity.User;
import com.example.SoftwareProjectHexashop.repository.OrderItemRepository;
import com.example.SoftwareProjectHexashop.repository.OrderRepository;
import com.example.SoftwareProjectHexashop.repository.ProductRepository;
import com.example.SoftwareProjectHexashop.repository.RoleRepository;
import com.example.SoftwareProjectHexashop.repository.UserRepository;

@SpringBootTest
@Transactional
class SecurityIntegrationTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(springSecurity())
                .build();

        orderItemRepository.deleteAllInBatch();
        orderRepository.deleteAllInBatch();
        productRepository.deleteAllInBatch();
        userRepository.deleteAllInBatch();
        roleRepository.deleteAllInBatch();
        roleRepository.flush();

        Role buyerRole = roleRepository.save(Role.builder().name(RoleName.BUYER).build());
        Role sellerRole = roleRepository.save(Role.builder().name(RoleName.SELLER).build());
        Role adminRole = roleRepository.save(Role.builder().name(RoleName.ADMIN).build());

        userRepository.save(User.builder()
                .fullName("Security Buyer")
                .email("buyer.security@example.com")
                .password(passwordEncoder.encode("Buyer@123"))
                .enabled(true)
                .roles(Set.of(buyerRole))
                .build());

        userRepository.save(User.builder()
                .fullName("Security Seller")
                .email("seller.security@example.com")
                .password(passwordEncoder.encode("Seller@123"))
                .enabled(true)
                .roles(Set.of(sellerRole))
                .build());

        userRepository.save(User.builder()
                .fullName("Security Admin")
                .email("admin.security@example.com")
                .password(passwordEncoder.encode("Admin@123"))
                .enabled(true)
                .roles(Set.of(adminRole))
                .build());
    }

    @Test
    void anonymousUserCannotAccessProtectedRoute() throws Exception {
        mockMvc.perform(get("/dashboard"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));
    }

    @Test
    void roleAwareDashboardRedirectionWorksEndToEnd() throws Exception {
        MockHttpSession buyerSession = login("buyer.security@example.com", "Buyer@123");
        mockMvc.perform(get("/dashboard").session(buyerSession))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/buyer/dashboard"));

        MockHttpSession sellerSession = login("seller.security@example.com", "Seller@123");
        mockMvc.perform(get("/dashboard").session(sellerSession))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/seller/dashboard"));

        MockHttpSession adminSession = login("admin.security@example.com", "Admin@123");
        mockMvc.perform(get("/dashboard").session(adminSession))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/dashboard"));
    }

    @Test
    void adminApiIsForbiddenForBuyerAndAllowedForAdmin() throws Exception {
        MockHttpSession buyerSession = login("buyer.security@example.com", "Buyer@123");
        mockMvc.perform(get("/api/admin/users").session(buyerSession))
                .andExpect(status().isForbidden());

        MockHttpSession adminSession = login("admin.security@example.com", "Admin@123");
        mockMvc.perform(get("/api/admin/users").session(adminSession))
                .andExpect(status().isOk());
    }

    private MockHttpSession login(String username, String password) throws Exception {
        MvcResult result = mockMvc.perform(formLogin().user(username).password(password))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        MockHttpSession session = (MockHttpSession) result.getRequest().getSession(false);
        assertNotNull(session);
        return session;
    }
}
