package com.example.SoftwareProjectHexashop.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class OrderRequest {
    @Valid
    @NotEmpty
    private List<OrderItemRequest> items;

    // Factory Pattern: encapsulates construction logic inside the class
    public static OrderRequest of(List<OrderItemRequest> items) {
        OrderRequest req = new OrderRequest();
        req.setItems(items);
        return req;
    }
}
