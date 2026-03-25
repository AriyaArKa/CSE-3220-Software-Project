package com.example.SoftwareProjectHexashop.service;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.SoftwareProjectHexashop.dto.OrderItemRequest;
import com.example.SoftwareProjectHexashop.dto.OrderRequest;
import com.example.SoftwareProjectHexashop.dto.OrderResponse;
import com.example.SoftwareProjectHexashop.entity.Order;
import com.example.SoftwareProjectHexashop.entity.OrderItem;
import com.example.SoftwareProjectHexashop.entity.OrderStatus;
import com.example.SoftwareProjectHexashop.entity.Product;
import com.example.SoftwareProjectHexashop.entity.User;
import com.example.SoftwareProjectHexashop.exception.ForbiddenOperationException;
import com.example.SoftwareProjectHexashop.repository.OrderRepository;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ProductService productService;

    @InjectMocks
    private OrderService orderService;

    @Test
    void checkoutCreatesOrderFromCartItems() {
        User buyer = user(20L, "buyer@example.com");
        User seller = user(50L, "seller@example.com");
        Product product = Product.builder()
                .id(7L)
                .name("Backpack")
                .price(new BigDecimal("1000.00"))
                .stock(10)
                .seller(seller)
                .build();

        when(productService.getProductEntity(7L)).thenReturn(product);
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> {
            Order order = invocation.getArgument(0);
            order.setId(99L);
            return order;
        });

        OrderRequest request = orderRequest(7L, 3);

        OrderResponse response = orderService.createOrder(request, buyer);

        assertEquals(99L, response.getId());
        assertEquals(OrderStatus.PENDING, response.getStatus());
        assertEquals(new BigDecimal("3000.00"), response.getTotalAmount());
        assertEquals(1, response.getItems().size());
        assertEquals(7, product.getStock());
    }

    @Test
    void buyerCanViewOnlyOwnOrders() {
        User buyer = user(5L, "buyer@example.com");
        User seller = user(8L, "seller@example.com");

        Order order = order(1L, buyer, seller, OrderStatus.PENDING);
        when(orderRepository.findByBuyer(buyer)).thenReturn(List.of(order));

        List<OrderResponse> orders = orderService.getOrdersForBuyer(buyer);

        assertEquals(1, orders.size());
        assertEquals("buyer@example.com", orders.get(0).getBuyerEmail());
        assertEquals(1L, orders.get(0).getId());
    }

    @Test
    void adminCanUpdateOrderStatus() {
        User buyer = user(10L, "buyer@example.com");
        User seller = user(11L, "seller@example.com");
        Order existing = order(12L, buyer, seller, OrderStatus.PENDING);

        when(orderRepository.findById(12L)).thenReturn(Optional.of(existing));
        when(orderRepository.save(existing)).thenReturn(existing);

        OrderResponse updated = orderService.updateStatus(12L, OrderStatus.SHIPPED);

        assertEquals(OrderStatus.SHIPPED, updated.getStatus());
        assertEquals(12L, updated.getId());
    }

    @Test
    void rejectOrderStatusUpdateByUnrelatedSeller() {
        User buyer = user(100L, "buyer@example.com");
        User ownerSeller = user(200L, "owner@example.com");
        User actorSeller = user(300L, "actor@example.com");

        Order existing = order(700L, buyer, ownerSeller, OrderStatus.PENDING);

        when(orderRepository.findById(700L)).thenReturn(Optional.of(existing));

        ForbiddenOperationException ex = assertThrows(ForbiddenOperationException.class,
                () -> orderService.updateStatusForSeller(700L, OrderStatus.SHIPPED, actorSeller));

        assertTrue(ex.getMessage().contains("relevant orders"));
    }

    private OrderRequest orderRequest(Long productId, Integer quantity) {
        OrderItemRequest item = new OrderItemRequest();
        item.setProductId(productId);
        item.setQuantity(quantity);

        OrderRequest request = new OrderRequest();
        request.setItems(List.of(item));
        return request;
    }

    private Order order(Long id, User buyer, User seller, OrderStatus status) {
        Product product = Product.builder()
                .id(90L)
                .name("Product")
                .price(new BigDecimal("500.00"))
                .stock(20)
                .seller(seller)
                .build();

        Order order = Order.builder()
                .id(id)
                .buyer(buyer)
                .status(status)
                .totalAmount(new BigDecimal("500.00"))
                .build();

        OrderItem item = OrderItem.builder()
                .order(order)
                .product(product)
                .quantity(1)
                .unitPrice(new BigDecimal("500.00"))
                .build();

        order.setItems(List.of(item));
        return order;
    }

    private User user(Long id, String email) {
        return User.builder()
                .id(id)
                .fullName("User")
                .email(email)
                .password("encoded")
                .enabled(true)
                .build();
    }
}
