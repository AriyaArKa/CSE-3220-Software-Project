package com.example.SoftwareProjectHexashop.service;

import java.math.BigDecimal;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpSession;

import com.example.SoftwareProjectHexashop.dto.OrderRequest;
import com.example.SoftwareProjectHexashop.dto.ProductResponse;

@ExtendWith(MockitoExtension.class)
class CartServiceTest {

    @Mock
    private ProductService productService;

    @Test
    void addProductToCart() {
        CartService cartService = new CartService(productService);
        MockHttpSession session = new MockHttpSession();
        when(productService.findById(5L)).thenReturn(product(5L, "Shirt", new BigDecimal("700.00"), 10));

        cartService.addItem(session, 5L, 2);

        Map<Long, Integer> cart = cartService.getCart(session);
        assertEquals(1, cart.size());
        assertEquals(2, cart.get(5L));
    }

    @Test
    void addSameProductTwiceMergesQuantity() {
        CartService cartService = new CartService(productService);
        MockHttpSession session = new MockHttpSession();
        when(productService.findById(3L)).thenReturn(product(3L, "Cap", new BigDecimal("350.00"), 10));

        cartService.addItem(session, 3L, 1);
        cartService.addItem(session, 3L, 3);

        assertEquals(4, cartService.getCart(session).get(3L));
    }

    @Test
    void updateOrRemoveCartItemCorrectly() {
        CartService cartService = new CartService(productService);
        MockHttpSession session = new MockHttpSession();
        when(productService.findById(11L)).thenReturn(product(11L, "Wallet", new BigDecimal("450.00"), 10));
        cartService.addItem(session, 11L, 1);

        cartService.updateItem(session, 11L, 5);
        assertEquals(5, cartService.getCart(session).get(11L));

        cartService.updateItem(session, 11L, 0);
        assertTrue(cartService.getCart(session).isEmpty());
    }

    @Test
    void toOrderRequestCreatesItemsFromCart() {
        CartService cartService = new CartService(productService);
        MockHttpSession session = new MockHttpSession();
        when(productService.findById(1L)).thenReturn(product(1L, "Watch", new BigDecimal("1200.00"), 10));
        when(productService.findById(2L)).thenReturn(product(2L, "Shoes", new BigDecimal("2200.00"), 10));

        cartService.addItem(session, 1L, 2);
        cartService.addItem(session, 2L, 1);

        OrderRequest orderRequest = cartService.toOrderRequest(session);

        assertEquals(2, orderRequest.getItems().size());
        assertTrue(orderRequest.getItems().stream().anyMatch(i -> i.getProductId().equals(1L) && i.getQuantity().equals(2)));
        assertTrue(orderRequest.getItems().stream().anyMatch(i -> i.getProductId().equals(2L) && i.getQuantity().equals(1)));
    }

    private ProductResponse product(Long id, String name, BigDecimal price, Integer stock) {
        return ProductResponse.builder()
                .id(id)
                .name(name)
                .price(price)
                .stock(stock)
                .imageUrl("/images/default.jpg")
                .build();
    }
}
