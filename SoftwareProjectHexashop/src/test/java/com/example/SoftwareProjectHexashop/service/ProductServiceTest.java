package com.example.SoftwareProjectHexashop.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.SoftwareProjectHexashop.dto.ProductRequest;
import com.example.SoftwareProjectHexashop.dto.ProductResponse;
import com.example.SoftwareProjectHexashop.entity.Product;
import com.example.SoftwareProjectHexashop.entity.User;
import com.example.SoftwareProjectHexashop.exception.ForbiddenOperationException;
import com.example.SoftwareProjectHexashop.repository.ProductRepository;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductService productService;

    @Captor
    private ArgumentCaptor<Product> productCaptor;

    @Test
    void createProductAsSeller() {
        User seller = user(10L, "seller@example.com");
        ProductRequest request = productRequest("Sneaker", "Lightweight", new BigDecimal("1500.00"), 8, null);

        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> {
            Product p = invocation.getArgument(0);
            p.setId(100L);
            return p;
        });

        ProductResponse response = productService.create(request, seller);

        verify(productRepository).save(productCaptor.capture());
        Product saved = productCaptor.getValue();
        assertEquals(seller, saved.getSeller());
        assertEquals("/images/men-01.jpg", saved.getImageUrl());
        assertEquals(100L, response.getId());
        assertEquals("seller@example.com", response.getSellerEmail());
    }

    @Test
    void rejectProductUpdateByNonOwnerSeller() {
        User owner = user(1L, "owner@example.com");
        User actor = user(2L, "other@example.com");
        Product existing = Product.builder()
                .id(50L)
                .name("Jacket")
                .description("Winter")
                .price(new BigDecimal("500.00"))
                .stock(2)
                .seller(owner)
                .build();

        when(productRepository.findById(50L)).thenReturn(Optional.of(existing));

        ProductRequest updateRequest = productRequest("Updated", "Updated", new BigDecimal("900.00"), 4, "/img.jpg");

        ForbiddenOperationException ex = assertThrows(ForbiddenOperationException.class,
                () -> productService.update(50L, updateRequest, actor, false));
        assertEquals("You can only edit your own products", ex.getMessage());
    }

    @Test
    void rejectProductDeletionByNonOwnerSeller() {
        User owner = user(1L, "owner@example.com");
        User actor = user(3L, "intruder@example.com");
        Product existing = Product.builder()
                .id(77L)
                .name("Watch")
                .price(new BigDecimal("250.00"))
                .stock(1)
                .seller(owner)
                .build();

        when(productRepository.findById(77L)).thenReturn(Optional.of(existing));

        ForbiddenOperationException ex = assertThrows(ForbiddenOperationException.class,
                () -> productService.delete(77L, actor, false));
        assertEquals("You can only delete your own products", ex.getMessage());
    }

    @Test
    void searchProductsByName() {
        User seller = user(1L, "seller@example.com");
        Product product = Product.builder()
                .id(9L)
                .name("Running Shoe")
                .description("Comfort")
                .price(new BigDecimal("1200.00"))
                .stock(5)
                .imageUrl("/images/shoe.jpg")
                .seller(seller)
                .build();

        when(productRepository.findByNameContainingIgnoreCase("shoe")).thenReturn(List.of(product));

        List<ProductResponse> results = productService.searchByName("shoe");

        assertEquals(1, results.size());
        assertEquals("Running Shoe", results.get(0).getName());
        assertEquals(9L, results.get(0).getId());
    }

    @Test
    void blankSearchFallsBackToFindAll() {
        User seller = user(8L, "seller@example.com");
        Product product = Product.builder()
                .id(10L)
                .name("Bag")
                .description("Travel")
                .price(new BigDecimal("999.00"))
                .stock(3)
                .imageUrl("/images/bag.jpg")
                .seller(seller)
                .build();

        when(productRepository.findAll()).thenReturn(List.of(product));

        List<ProductResponse> results = productService.searchByName("   ");

        assertEquals(1, results.size());
        assertTrue(results.stream().anyMatch(p -> p.getName().equals("Bag")));
    }

    private User user(Long id, String email) {
        return User.builder()
                .id(id)
                .email(email)
                .fullName("User")
                .password("encoded")
                .enabled(true)
                .build();
    }

    private ProductRequest productRequest(String name, String description, BigDecimal price, Integer stock, String imageUrl) {
        ProductRequest request = new ProductRequest();
        request.setName(name);
        request.setDescription(description);
        request.setPrice(price);
        request.setStock(stock);
        request.setImageUrl(imageUrl);
        return request;
    }
}
