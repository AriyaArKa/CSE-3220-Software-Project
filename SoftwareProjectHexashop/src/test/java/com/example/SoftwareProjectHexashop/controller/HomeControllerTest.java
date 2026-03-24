package com.example.SoftwareProjectHexashop.controller;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class HomeControllerTest {

    private final HomeController homeController = new HomeController();

    @Test
    void indexRouteReturnsIndexView() {
        assertEquals("index", homeController.index());
    }

    @Test
    void productsRouteReturnsProductsView() {
        assertEquals("products", homeController.products());
    }

    @Test
    void productRouteReturnsSingleProductView() {
        assertEquals("single-product", homeController.singleProduct());
    }

    @Test
    void ordersRouteReturnsOrdersView() {
        assertEquals("orders", homeController.orders());
    }
}
