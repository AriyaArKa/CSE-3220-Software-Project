package com.example.SoftwareProjectHexashop.integration;

import java.math.BigDecimal;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
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

import com.example.SoftwareProjectHexashop.entity.Order;
import com.example.SoftwareProjectHexashop.entity.OrderStatus;
import com.example.SoftwareProjectHexashop.entity.Product;
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
class ProductLifecycleIntegrationTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private OrderItemRepository orderItemRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private Product product;

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
                .fullName("Lifecycle Buyer")
                .email("buyer.lifecycle@example.com")
                .password(passwordEncoder.encode("Buyer@123"))
                .enabled(true)
                .roles(Set.of(buyerRole))
                .build());

        userRepository.save(User.builder()
                .fullName("Lifecycle Admin")
                .email("admin.lifecycle.orders@example.com")
                .password(passwordEncoder.encode("Admin@123"))
                .enabled(true)
                .roles(Set.of(adminRole))
                .build());

        User seller = userRepository.save(User.builder()
                .fullName("Lifecycle Seller")
                .email("seller.lifecycle.orders@example.com")
                .password(passwordEncoder.encode("Seller@123"))
                .enabled(true)
                .roles(Set.of(sellerRole))
                .build());

        product = productRepository.save(Product.builder()
                .name("Lifecycle Product")
                .description("Used for order lifecycle integration test")
                .price(new BigDecimal("350.00"))
                .stock(5)
                .imageUrl("/images/men-01.jpg")
                .seller(seller)
                .build());
    }

    @Test
    void productLifecycleBuyerCheckoutAndAdminDeliversOrder() throws Exception {
        MockHttpSession buyerSession = login("buyer.lifecycle@example.com", "Buyer@123");

        mockMvc.perform(post("/cart/add")
                .session(buyerSession)
                .with(csrf())
                .param("productId", product.getId().toString())
                .param("quantity", "1")
                .header("Referer", "/products"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/products"));

        mockMvc.perform(post("/cart/checkout")
                .session(buyerSession)
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/orders"));

        Order order = orderRepository.findAll().get(0);
        assertEquals(OrderStatus.PENDING, order.getStatus());

        mockMvc.perform(get("/orders").session(buyerSession))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("PENDING")));

        MockHttpSession adminSession = login("admin.lifecycle.orders@example.com", "Admin@123");

        mockMvc.perform(get("/admin/dashboard").session(adminSession))
                .andExpect(status().isOk());

        mockMvc.perform(post("/admin/orders/{id}/status", order.getId())
                .session(adminSession)
                .with(csrf())
                .param("status", "DELIVERED"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/admin/dashboard"));

        Order updatedOrder = orderRepository.findById(order.getId()).orElseThrow();
        assertEquals(OrderStatus.DELIVERED, updatedOrder.getStatus());

        mockMvc.perform(get("/orders").session(buyerSession))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("DELIVERED")));

        assertTrue(orderRepository.findByBuyer(userRepository.findByEmail("buyer.lifecycle@example.com").orElseThrow())
                .stream()
                .anyMatch(o -> o.getId().equals(order.getId()) && o.getStatus() == OrderStatus.DELIVERED));
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
