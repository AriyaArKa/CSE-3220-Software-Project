package com.example.SoftwareProjectHexashop.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import com.example.SoftwareProjectHexashop.dto.ProductRequest;
import com.example.SoftwareProjectHexashop.dto.ProductResponse;
import com.example.SoftwareProjectHexashop.entity.Product;
import com.example.SoftwareProjectHexashop.entity.User;
import com.example.SoftwareProjectHexashop.exception.ForbiddenOperationException;
import com.example.SoftwareProjectHexashop.exception.ResourceNotFoundException;
import com.example.SoftwareProjectHexashop.repository.ProductRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class ProductService {

    private final ProductRepository productRepository;

    @Transactional(readOnly = true)
    public List<ProductResponse> findAll() {
        return productRepository.findAll().stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> searchByName(String query) {
        if (query == null || query.isBlank()) {
            return findAll();
        }
        return productRepository.findByNameContainingIgnoreCase(query).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public ProductResponse findById(Long id) {
        return toResponse(getProductEntity(id));
    }

    @Transactional
    public ProductResponse create(ProductRequest request, User seller) {
        return create(request, seller, null);
    }

    @Transactional
    public ProductResponse create(ProductRequest request, User seller, MultipartFile imageFile) {
        String imageUrl = resolveImageUrl(request.getImageUrl(), imageFile);

        Product product = Product.builder()
                .name(request.getName())
                .description(request.getDescription())
                .price(request.getPrice())
                .stock(request.getStock())
                .imageUrl(imageUrl)
                .seller(seller)
                .build();

        return toResponse(productRepository.save(product));
    }

    @Transactional
    public ProductResponse update(Long id, ProductRequest request, User actor, boolean admin) {
        Product product = getProductEntity(id);

        if (!admin && !product.getSeller().getId().equals(actor.getId())) {
            throw new ForbiddenOperationException("You can only edit your own products");
        }

        product.setName(request.getName());
        product.setDescription(request.getDescription());
        product.setPrice(request.getPrice());
        product.setStock(request.getStock());
        product.setImageUrl(request.getImageUrl());

        return toResponse(productRepository.save(product));
    }

    @Transactional
    public void delete(Long id, User actor, boolean admin) {
        Product product = getProductEntity(id);
        if (!admin && !product.getSeller().getId().equals(actor.getId())) {
            throw new ForbiddenOperationException("You can only delete your own products");
        }
        productRepository.delete(product);
    }

    @Transactional
    public ProductResponse restock(Long id, Integer amount, User actor, boolean admin) {
        Product product = getProductEntity(id);

        if (!admin && !product.getSeller().getId().equals(actor.getId())) {
            throw new ForbiddenOperationException("You can only restock your own products");
        }

        int increaseBy = (amount == null || amount < 1) ? 1 : amount;
        Integer stock = product.getStock();
        int currentStock = stock == null ? 0 : stock;
        product.setStock(currentStock + increaseBy);

        return toResponse(productRepository.save(product));
    }

    @Transactional(readOnly = true)
    public List<ProductResponse> findBySeller(User seller) {
        return productRepository.findBySeller(seller).stream().map(this::toResponse).toList();
    }

    @Transactional(readOnly = true)
    public Product getProductEntity(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found: " + id));
    }

    private ProductResponse toResponse(Product product) {
        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .price(product.getPrice())
                .stock(product.getStock())
                .imageUrl(product.getImageUrl())
                .sellerEmail(product.getSeller() == null ? null : product.getSeller().getEmail())
                .build();
    }

    private String resolveImageUrl(String imageUrlInput, MultipartFile imageFile) {
        if (imageFile != null && !imageFile.isEmpty()) {
            return storeUploadedImage(imageFile);
        }

        if (imageUrlInput == null || imageUrlInput.isBlank()) {
            return "/images/men-01.jpg";
        }
        return imageUrlInput.trim();
    }

    private String storeUploadedImage(MultipartFile imageFile) {
        String contentType = imageFile.getContentType();
        if (contentType == null || !contentType.toLowerCase().startsWith("image/")) {
            throw new IllegalArgumentException("Uploaded file must be an image");
        }

        String originalName = StringUtils.cleanPath(imageFile.getOriginalFilename() == null ? "" : imageFile.getOriginalFilename());
        String extension = "";
        int dotIndex = originalName.lastIndexOf('.');
        if (dotIndex >= 0 && dotIndex < originalName.length() - 1) {
            extension = originalName.substring(dotIndex);
        }

        String fileName = UUID.randomUUID() + extension;
        Path uploadDir = Paths.get("uploads", "products").toAbsolutePath().normalize();
        Path destination = uploadDir.resolve(fileName).normalize();

        try {
            Files.createDirectories(uploadDir);
            imageFile.transferTo(destination);
        } catch (IOException ex) {
            throw new IllegalArgumentException("Failed to store uploaded image");
        }

        return "/uploads/products/" + fileName;
    }
}
