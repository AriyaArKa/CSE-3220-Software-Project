package com.example.SoftwareProjectHexashop.controller;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import com.example.SoftwareProjectHexashop.dto.ProductRequest;
import com.example.SoftwareProjectHexashop.dto.RegisterRequest;
import com.example.SoftwareProjectHexashop.entity.OrderStatus;
import com.example.SoftwareProjectHexashop.entity.RoleName;
import com.example.SoftwareProjectHexashop.entity.User;
import com.example.SoftwareProjectHexashop.service.OrderService;
import com.example.SoftwareProjectHexashop.service.ProductService;
import com.example.SoftwareProjectHexashop.service.UserService;

import lombok.RequiredArgsConstructor;

@Controller
@RequiredArgsConstructor
public class DashboardController {

    private final UserService userService;
    private final ProductService productService;
    private final OrderService orderService;

    @GetMapping("/dashboard")
    public String dashboard(Authentication authentication) {
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
        boolean isSeller = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_SELLER"));

        if (isAdmin) {
            return "redirect:/admin/dashboard";
        }
        if (isSeller) {
            return "redirect:/seller/dashboard";
        }
        return "redirect:/buyer/dashboard";
    }

    @GetMapping("/admin/dashboard")
    public String adminDashboard(Model model) {
        model.addAttribute("users", userService.getAllUsers());
        model.addAttribute("sellers", userService.getUsersByRole(RoleName.SELLER));
        model.addAttribute("pendingSellers", userService.getPendingSellerApprovals());
        model.addAttribute("buyers", userService.getUsersByRole(RoleName.BUYER));
        model.addAttribute("orders", orderService.getAllOrders());
        model.addAttribute("pendingOrders", orderService.getPendingOrders());
        model.addAttribute("products", productService.findAll());
        if (!model.containsAttribute("sellerRequest")) {
            model.addAttribute("sellerRequest", new RegisterRequest());
        }
        model.addAttribute("orderStatuses", OrderStatus.values());
        return "dashboard/admin-dashboard";
    }

    @GetMapping("/seller/dashboard")
    public String sellerDashboard(Authentication authentication, Model model) {
        User seller = userService.findByEmail(authentication.getName());
        model.addAttribute("products", productService.findBySeller(seller));
        model.addAttribute("pendingOrders", orderService.getPendingOrdersForSeller(seller));
        model.addAttribute("orderStatuses", OrderStatus.values());
        if (!model.containsAttribute("productRequest")) {
            model.addAttribute("productRequest", new ProductRequest());
        }
        return "dashboard/seller-dashboard";
    }

    @GetMapping("/buyer/dashboard")
    public String buyerDashboard(Authentication authentication, Model model) {
        User buyer = userService.findByEmail(authentication.getName());
        model.addAttribute("orders", orderService.getOrdersForBuyer(buyer));
        model.addAttribute("products", productService.findAll());
        return "dashboard/buyer-dashboard";
    }

    @GetMapping("/orders")
    public String myOrders(Authentication authentication, Model model) {
        User buyer = userService.findByEmail(authentication.getName());
        model.addAttribute("orders", orderService.getOrdersForBuyer(buyer));
        return "orders";
    }

    @PostMapping("/admin/sellers")
    @PreAuthorize("hasRole('ADMIN')")
    public String addSeller(@ModelAttribute("sellerRequest") RegisterRequest sellerRequest,
            RedirectAttributes redirectAttributes) {
        try {
            userService.createSellerByAdmin(sellerRequest.getFullName(), sellerRequest.getEmail(), sellerRequest.getPassword());
            redirectAttributes.addFlashAttribute("successMessage", "Seller account created successfully.");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            redirectAttributes.addFlashAttribute("sellerRequest", sellerRequest);
        }
        return "redirect:/admin/dashboard";
    }

    @PostMapping("/admin/sellers/{id}/remove")
    @PreAuthorize("hasRole('ADMIN')")
    public String removeSeller(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        userService.removeSellerRole(id);
        redirectAttributes.addFlashAttribute("successMessage", "Seller role removed from user.");
        return "redirect:/admin/dashboard";
    }

    @PostMapping("/admin/sellers/{id}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    public String approveSeller(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        userService.approveSeller(id);
        redirectAttributes.addFlashAttribute("successMessage", "Seller approved successfully.");
        return "redirect:/admin/dashboard";
    }

    @PostMapping("/admin/orders/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public String updateOrderStatus(@PathVariable Long id,
            @ModelAttribute("status") OrderStatus status,
            RedirectAttributes redirectAttributes) {
        orderService.updateStatus(id, status);
        redirectAttributes.addFlashAttribute("successMessage", "Order status updated.");
        return "redirect:/admin/dashboard";
    }

    @PostMapping("/seller/orders/{id}/status")
    @PreAuthorize("hasAnyRole('SELLER','ADMIN')")
    public String updateOrderStatusAsSeller(@PathVariable Long id,
            @ModelAttribute("status") OrderStatus status,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        User seller = userService.findByEmail(authentication.getName());
        orderService.updateStatusForSeller(id, status, seller);
        redirectAttributes.addFlashAttribute("successMessage", "Order status updated.");
        return "redirect:/seller/dashboard";
    }

    @PostMapping("/admin/products/{id}/delete")
    @PreAuthorize("hasRole('ADMIN')")
    public String deleteProductAsAdmin(@PathVariable Long id,
            Authentication authentication,
            RedirectAttributes redirectAttributes) {
        User admin = userService.findByEmail(authentication.getName());
        productService.delete(id, admin, true);
        redirectAttributes.addFlashAttribute("successMessage", "Product removed by admin.");
        return "redirect:/admin/dashboard";
    }
}
