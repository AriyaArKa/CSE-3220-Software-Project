package com.example.SoftwareProjectHexashop.controller;

import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.SoftwareProjectHexashop.dto.ProductRequest;
import com.example.SoftwareProjectHexashop.entity.User;
import com.example.SoftwareProjectHexashop.service.ProductService;
import com.example.SoftwareProjectHexashop.service.UserService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class SellerProductController {

    private final ProductService productService;
    private final UserService userService;

    @PostMapping("/seller/products")
    public String createProduct(@Valid @ModelAttribute("productRequest") ProductRequest productRequest,
            BindingResult bindingResult,
            @RequestParam(value = "imageFile", required = false) MultipartFile imageFile,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        if (bindingResult.hasErrors()) {
            redirectAttributes.addFlashAttribute("org.springframework.validation.BindingResult.productRequest", bindingResult);
            redirectAttributes.addFlashAttribute("productRequest", productRequest);
            redirectAttributes.addFlashAttribute("errorMessage", "Please correct the form errors before saving.");
            return "redirect:/seller/dashboard";
        }

        User seller = userService.findByEmail(authentication.getName());
        try {
            productService.create(productRequest, seller, imageFile);
            redirectAttributes.addFlashAttribute("successMessage", "Product added successfully.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("productRequest", productRequest);
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
        }
        return "redirect:/seller/dashboard";
    }

    @PostMapping("/seller/products/{id}/delete")
    public String deleteProduct(@PathVariable Long id,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        User actor = userService.findByEmail(authentication.getName());
        boolean admin = authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        productService.delete(id, actor, admin);
        redirectAttributes.addFlashAttribute("successMessage", "Product deleted successfully.");
        return "redirect:/seller/dashboard";
    }

    @PostMapping("/seller/products/{id}/restock")
    public String restockProduct(@PathVariable Long id,
            @RequestParam(defaultValue = "1") Integer amount,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        User actor = userService.findByEmail(authentication.getName());
        boolean admin = authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        productService.restock(id, amount, actor, admin);
        redirectAttributes.addFlashAttribute("successMessage", "Product restocked successfully.");
        return "redirect:/seller/dashboard";
    }
}
