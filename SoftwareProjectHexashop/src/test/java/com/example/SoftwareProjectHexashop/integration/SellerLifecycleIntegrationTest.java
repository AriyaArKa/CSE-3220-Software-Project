package com.example.SoftwareProjectHexashop.integration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.crypto.password.PasswordEncoder;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestBuilders.formLogin;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
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
class SellerLifecycleIntegrationTest {

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

        Role adminRole = roleRepository.save(Role.builder().name(RoleName.ADMIN).build());
        roleRepository.save(Role.builder().name(RoleName.SELLER).build());

        userRepository.save(User.builder()
                .fullName("Integration Admin")
                .email("admin.lifecycle@example.com")
                .password(passwordEncoder.encode("Admin@123"))
                .enabled(true)
                .roles(java.util.Set.of(adminRole))
                .build());
    }

    @Test
    void sellerLifecycleRegisterApproveAndCreateProduct() throws Exception {
        String sellerEmail = "seller.lifecycle@example.com";

        mockMvc.perform(post("/register")
                .with(csrf())
                .param("fullName", "Pending Seller")
                .param("email", sellerEmail)
                .param("password", "Seller@123")
                .param("role", "SELLER"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?pendingApproval"));

        User pendingSeller = userRepository.findByEmail(sellerEmail).orElseThrow();
        assertFalse(pendingSeller.isEnabled());

        mockMvc.perform(formLogin().user(sellerEmail).password("Seller@123"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login?error"));

        mockMvc.perform(post("/seller/products")
                .with(csrf())
                .param("name", "Pending Product")
                .param("description", "Should not be created")
                .param("price", "100.00")
                .param("stock", "5")
                .param("imageUrl", "/images/men-01.jpg"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/login"));

        MockHttpSession adminSession = login("admin.lifecycle@example.com", "Admin@123");

        mockMvc.perform(get("/admin/dashboard").session(adminSession))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString(sellerEmail)));

        mockMvc.perform(post("/admin/sellers/{id}/approve", pendingSeller.getId())
                .session(adminSession)
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/dashboard"));

        User approvedSeller = userRepository.findByEmail(sellerEmail).orElseThrow();
        assertTrue(approvedSeller.isEnabled());

        MockHttpSession sellerSession = login(sellerEmail, "Seller@123");

        mockMvc.perform(get("/seller/dashboard").session(sellerSession))
                .andExpect(status().isOk());

        mockMvc.perform(post("/seller/products")
                .session(sellerSession)
                .with(csrf())
                .param("name", "Approved Seller Product")
                .param("description", "Now allowed")
                .param("price", "1400.00")
                .param("stock", "7")
                .param("imageUrl", "/images/men-01.jpg"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/seller/dashboard"));

        assertEquals(1, productRepository.findBySeller(approvedSeller).size());
        assertEquals("Approved Seller Product", productRepository.findBySeller(approvedSeller).get(0).getName());
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
