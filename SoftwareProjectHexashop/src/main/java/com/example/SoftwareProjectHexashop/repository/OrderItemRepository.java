package com.example.SoftwareProjectHexashop.repository;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.SoftwareProjectHexashop.entity.OrderItem;

public interface OrderItemRepository extends JpaRepository<OrderItem, Long> {
}
