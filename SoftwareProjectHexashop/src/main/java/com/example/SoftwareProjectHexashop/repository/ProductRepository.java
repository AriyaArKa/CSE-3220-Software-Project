package com.example.SoftwareProjectHexashop.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.example.SoftwareProjectHexashop.entity.Product;
import com.example.SoftwareProjectHexashop.entity.User;

public interface ProductRepository extends JpaRepository<Product, Long> {
    List<Product> findBySeller(User seller);
    List<Product> findByNameContainingIgnoreCase(String name);
}
