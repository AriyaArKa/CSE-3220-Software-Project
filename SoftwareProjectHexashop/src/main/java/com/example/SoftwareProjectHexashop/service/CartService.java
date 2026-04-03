package com.example.SoftwareProjectHexashop.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.example.SoftwareProjectHexashop.dto.CartItemView;
import com.example.SoftwareProjectHexashop.dto.OrderItemRequest;
import com.example.SoftwareProjectHexashop.dto.OrderRequest;
import com.example.SoftwareProjectHexashop.dto.ProductResponse;

import jakarta.servlet.http.HttpSession;

@Service
public class CartService {

    private static final String CART_SESSION_KEY = "SESSION_CART";
    private final ProductService productService;

    public CartService(ProductService productService) {
        this.productService = productService;
    }

    @SuppressWarnings("unchecked")
    public Map<Long, Integer> getCart(HttpSession session) {
        Object existing = session.getAttribute(CART_SESSION_KEY);
        if (existing instanceof Map<?, ?> existingMap) {
            return (Map<Long, Integer>) existingMap;
        }
        Map<Long, Integer> cart = new LinkedHashMap<>();
        session.setAttribute(CART_SESSION_KEY, cart);
        return cart;
    }

    public void addItem(HttpSession session, Long productId, Integer quantity) {
        int qty = quantity == null || quantity < 1 ? 1 : quantity;
        Map<Long, Integer> cart = getCart(session);
        ProductResponse product = productService.findById(productId);
        Integer productStock = product.getStock();
        int availableStock = productStock == null ? 0 : productStock;
        int existingQty = cart.getOrDefault(productId, 0);

        if (availableStock <= 0) {
            throw new IllegalArgumentException("Product is out of stock.");
        }

        if (existingQty + qty > availableStock) {
            throw new IllegalArgumentException("Only " + availableStock + " item(s) available in stock.");
        }

        cart.put(productId, cart.getOrDefault(productId, 0) + qty);
    }

    public void updateItem(HttpSession session, Long productId, Integer quantity) {
        Map<Long, Integer> cart = getCart(session);
        if (quantity == null || quantity < 1) {
            cart.remove(productId);
            return;
        }
        cart.put(productId, quantity);
    }

    public void removeItem(HttpSession session, Long productId) {
        getCart(session).remove(productId);
    }

    public void clear(HttpSession session) {
        getCart(session).clear();
    }

    public int getItemCount(HttpSession session) {
        return getCart(session).values().stream().mapToInt(Integer::intValue).sum();
    }

    public List<CartItemView> getCartItems(HttpSession session) {
        List<CartItemView> items = new ArrayList<>();
        for (Map.Entry<Long, Integer> entry : getCart(session).entrySet()) {
            ProductResponse product = productService.findById(entry.getKey());
            int qty = entry.getValue();
            BigDecimal lineTotal = product.getPrice().multiply(BigDecimal.valueOf(qty));

            items.add(CartItemView.builder()
                    .productId(product.getId())
                    .name(product.getName())
                    .imageUrl(product.getImageUrl())
                    .unitPrice(product.getPrice())
                    .quantity(qty)
                    .lineTotal(lineTotal)
                    .build());
        }
        return items;
    }

    public BigDecimal getCartTotal(HttpSession session) {
        return getCartItems(session).stream()
                .map(CartItemView::getLineTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public OrderRequest toOrderRequest(HttpSession session) {
        List<OrderItemRequest> orderItems = new ArrayList<>();
        for (Map.Entry<Long, Integer> entry : getCart(session).entrySet()) {
            orderItems.add(OrderItemRequest.of(entry.getKey(), entry.getValue()));
        }
        return OrderRequest.of(orderItems);
    }
}
