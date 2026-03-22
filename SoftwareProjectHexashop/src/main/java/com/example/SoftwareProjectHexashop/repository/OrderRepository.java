package com.example.SoftwareProjectHexashop.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.SoftwareProjectHexashop.entity.Order;
import com.example.SoftwareProjectHexashop.entity.OrderStatus;
import com.example.SoftwareProjectHexashop.entity.User;

public interface OrderRepository extends JpaRepository<Order, Long> {

    List<Order> findByBuyer(User buyer);

    List<Order> findByStatus(OrderStatus status);

    List<Order> findDistinctByItemsProductSeller(User seller);

    List<Order> findDistinctByItemsProductSellerAndStatus(User seller, OrderStatus status);
}
