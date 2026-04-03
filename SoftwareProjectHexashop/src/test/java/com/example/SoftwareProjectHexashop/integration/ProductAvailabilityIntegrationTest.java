package com.example.SoftwareProjectHexashop.integration;

import java.math.BigDecimal;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.context.WebApplicationContext;

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
class ProductAvailabilityIntegrationTest {

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

        userRepository.save(User.builder()
                .fullName("Buyer One")
                .email("buyer.one.availability@example.com")
                .password(passwordEncoder.encode("Buyer@123"))
                .enabled(true)
                .roles(Set.of(buyerRole))
                .build());

        userRepository.save(User.builder()
                .fullName("Buyer Two")
                .email("buyer.two.availability@example.com")
                .password(passwordEncoder.encode("Buyer@123"))
                .enabled(true)
                .roles(Set.of(buyerRole))
                .build());

        User seller = userRepository.save(User.builder()
                .fullName("Verified Seller")
                .email("seller.availability@example.com")
                .password(passwordEncoder.encode("Seller@123"))
                .enabled(true)
                .roles(Set.of(sellerRole))
                .build());

        product = productRepository.save(Product.builder()
                .name("Limited Product")
                .description("Used for stock availability integration test")
                .price(new BigDecimal("500.00"))
                .stock(2)
                .imageUrl("/images/men-01.jpg")
                .seller(seller)
                .build());
    }

    @Test
    void productAvailabilityRestockFlowWorks() throws Exception {
        MockHttpSession buyerOneSession = login("buyer.one.availability@example.com", "Buyer@123");

        mockMvc.perform(post("/cart/add")
                .session(buyerOneSession)
                .with(csrf())
                .param("productId", product.getId().toString())
                .param("quantity", "2")
                .header("Referer", "/products"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/products"));

        mockMvc.perform(post("/cart/checkout")
                .session(buyerOneSession)
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/orders"));

        assertEquals(1, orderRepository.findAll().size());
        assertEquals(0, productRepository.findById(product.getId()).orElseThrow().getStock());

        MockHttpSession buyerTwoSession = login("buyer.two.availability@example.com", "Buyer@123");

        mockMvc.perform(post("/cart/add")
                .session(buyerTwoSession)
                .with(csrf())
                .param("productId", product.getId().toString())
                .param("quantity", "1")
                .header("Referer", "/products"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/products"));

        mockMvc.perform(post("/cart/checkout")
                .session(buyerTwoSession)
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/cart"));

        assertEquals(1, orderRepository.findAll().size());

        MockHttpSession sellerSession = login("seller.availability@example.com", "Seller@123");

        mockMvc.perform(post("/seller/products/{id}/restock", product.getId())
                .session(sellerSession)
                .with(csrf())
                .param("amount", "4"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/seller/dashboard"));

        assertEquals(4, productRepository.findById(product.getId()).orElseThrow().getStock());

        mockMvc.perform(post("/cart/add")
                .session(buyerTwoSession)
                .with(csrf())
                .param("productId", product.getId().toString())
                .param("quantity", "1")
                .header("Referer", "/products"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/products"));

        mockMvc.perform(post("/cart/checkout")
                .session(buyerTwoSession)
                .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/orders"));

        assertEquals(2, orderRepository.findAll().size());
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
