package com.example.SoftwareProjectHexashop.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class OrderItemRequest {
    @NotNull
    private Long productId;

    @NotNull
    @Min(1)
    private Integer quantity;

    // Factory Pattern: encapsulates construction logic inside the class
    public static OrderItemRequest of(Long productId, Integer quantity) {
        OrderItemRequest req = new OrderItemRequest();
        req.setProductId(productId);
        req.setQuantity(quantity);
        return req;
    }
}
