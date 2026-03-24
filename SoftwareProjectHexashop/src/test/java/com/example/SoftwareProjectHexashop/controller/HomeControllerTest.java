package com.example.SoftwareProjectHexashop.controller;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ui.ExtendedModelMap;
import org.springframework.ui.Model;

import com.example.SoftwareProjectHexashop.dto.ProductResponse;
import com.example.SoftwareProjectHexashop.service.ProductService;

@ExtendWith(MockitoExtension.class)
class HomeControllerTest {

    @Mock
    private ProductService productService;

    @InjectMocks
    private HomeController homeController;

    @Test
    void homeRouteReturnsIndexView() {
        Model model = new ExtendedModelMap();
        when(productService.findAll()).thenReturn(List.of());

        String view = homeController.home(model);

        assertEquals("index", view);
        assertTrue(model.containsAttribute("products"));
    }

    @Test
    void productsRouteReturnsProductsView() {
        Model model = new ExtendedModelMap();
        when(productService.searchByName("shoe")).thenReturn(List.of());

        String view = homeController.products("shoe", model);

        assertEquals("products", view);
        assertTrue(model.containsAttribute("products"));
        assertEquals("shoe", model.getAttribute("q"));
    }

    @Test
    void productRouteReturnsSingleProductView() {
        Model model = new ExtendedModelMap();
        when(productService.findById(1L)).thenReturn(ProductResponse.builder().id(1L).name("Demo").build());

        String view = homeController.productDetails(1L, model);

        assertEquals("single-product", view);
        assertTrue(model.containsAttribute("product"));
    }

    @Test
    void aboutRouteReturnsAboutView() {
        assertEquals("about", homeController.about());
    }

    @Test
    void contactRouteReturnsContactView() {
        assertEquals("contact", homeController.contact());
    }
}
