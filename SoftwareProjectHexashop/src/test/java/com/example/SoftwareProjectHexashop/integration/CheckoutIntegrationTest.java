package com.example.SoftwareProjectHexashop.integration;

import java.math.BigDecimal;
import java.util.List;
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
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

import com.example.SoftwareProjectHexashop.entity.Order;
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
class CheckoutIntegrationTest {

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

        orderItemRepository.deleteAll();
        orderRepository.deleteAll();
        productRepository.deleteAll();
        userRepository.deleteAll();
        roleRepository.deleteAll();

        Role buyerRole = roleRepository.save(Role.builder().name(RoleName.BUYER).build());
        Role sellerRole = roleRepository.save(Role.builder().name(RoleName.SELLER).build());

        userRepository.save(User.builder()
                .fullName("Integration Buyer")
                .email("buyer.integration@example.com")
                .password(passwordEncoder.encode("Buyer@123"))
                .enabled(true)
                .roles(Set.of(buyerRole))
                .build());

        User seller = userRepository.save(User.builder()
                .fullName("Integration Seller")
                .email("seller.integration@example.com")
                .password(passwordEncoder.encode("Seller@123"))
                .enabled(true)
                .roles(Set.of(sellerRole))
                .build());

        product = productRepository.save(Product.builder()
                .name("Flow Product")
                .description("Used for checkout flow integration test")
                .price(new BigDecimal("250.00"))
                .stock(30)
                .imageUrl("/images/men-01.jpg")
                .seller(seller)
                .build());
    }

    @Test
    void buyerPurchaseFlowWorksEndToEnd() throws Exception {
        MvcResult loginResult = mockMvc.perform(formLogin()
                .user("buyer.integration@example.com")
                .password("Buyer@123"))
                .andExpect(status().is3xxRedirection())
                .andReturn();

        MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);
        assertNotNull(session);

        mockMvc.perform(post("/cart/add")
                .session(session)
                .with(csrf())
                .param("productId", product.getId().toString())
                .param("quantity", "2")
                .header("Referer", "/products"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/products"));

        mockMvc.perform(get("/cart").session(session))
                .andExpect(status().isOk());

        mockMvc.perform(post("/cart/checkout")
                .session(session)
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/orders"));

        mockMvc.perform(get("/orders").session(session))
                .andExpect(status().isOk());

        List<Order> orders = orderRepository.findAll();
        assertEquals(1, orders.size());
        assertEquals("buyer.integration@example.com", orders.get(0).getBuyer().getEmail());
        assertTrue(orders.get(0).getItems().stream().anyMatch(i -> i.getProduct().getId().equals(product.getId())));
    }
}
