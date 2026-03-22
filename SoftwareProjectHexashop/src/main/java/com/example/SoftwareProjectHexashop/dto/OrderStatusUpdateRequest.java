package com.example.SoftwareProjectHexashop.dto;

import com.example.SoftwareProjectHexashop.entity.OrderStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class OrderStatusUpdateRequest {
    @NotNull
    private OrderStatus status;
}
