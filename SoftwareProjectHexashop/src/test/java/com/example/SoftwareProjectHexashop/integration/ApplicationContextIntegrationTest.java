package com.example.SoftwareProjectHexashop.integration;

import java.math.BigDecimal;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.crypto.password.PasswordEncoder;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;
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
class ApplicationContextIntegrationTest {

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

        Role sellerRole = roleRepository.save(Role.builder().name(RoleName.SELLER).build());

        User seller = userRepository.save(User.builder()
                .fullName("Integration Seller")
                .email("seller.integration@example.com")
                .password(passwordEncoder.encode("Seller@123"))
                .enabled(true)
                .roles(Set.of(sellerRole))
                .build());

        product = productRepository.save(Product.builder()
                .name("Integration Product")
                .description("Used for public page integration tests")
                .price(new BigDecimal("499.99"))
                .stock(25)
                .imageUrl("/images/men-01.jpg")
                .seller(seller)
                .build());
    }

    @Test
    void applicationContextAndPublicPagesLoadSuccessfully() throws Exception {
        mockMvc.perform(get("/"))
                .andExpect(status().isOk())
                .andExpect(view().name("index"));

        mockMvc.perform(get("/products"))
                .andExpect(status().isOk())
                .andExpect(view().name("products"));

        mockMvc.perform(get("/products/{id}", product.getId()))
                .andExpect(status().isOk())
                .andExpect(view().name("single-product"));
    }
}
