package org.example.mvc_tg_bot.controller;


import lombok.RequiredArgsConstructor;
import org.example.mvc_tg_bot.model.OrderStatus;
import org.example.mvc_tg_bot.service.OrderService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.example.mvc_tg_bot.model.Product;
import org.example.mvc_tg_bot.service.ProductService;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class WebController {

    private final ProductService productService;
    private final OrderService orderService;

    @GetMapping("/products")
    public String productsPage(Model model) {
        model.addAttribute("products", productService.getActiveProducts());
        return "admin/products";
    }

    @PostMapping("/products")
    public String addProduct(@ModelAttribute Product product) {
        productService.save(product);
        return "redirect:/admin/products";
    }

    @GetMapping("/products/delete/{id}")
    public String deleteProduct(@PathVariable Long id) {
        productService.delete(id);
        return "redirect:/admin/products";
    }

    @GetMapping
    public String adminHome() {
        return "redirect:/admin/products";
    }

    @GetMapping("/orders")
    public String ordersPage(Model model) {
        model.addAttribute("orders", orderService.getAllOrders());
        return "admin/orders";
    }

    @GetMapping("/orders/status/{id}")
    public String updateStatus(@PathVariable Long id, @RequestParam OrderStatus status) {
        orderService.updateStatus(id, status);
        return "redirect:/admin/orders";
    }
}