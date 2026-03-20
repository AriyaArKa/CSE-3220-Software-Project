package com.example.SoftwareProjectHexashop.repository;

import com.example.SoftwareProjectHexashop.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductRepository extends JpaRepository<Product, Long> {
}



