package org.example.mvc_tg_bot.service;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.example.mvc_tg_bot.model.*;
import org.example.mvc_tg_bot.repository.OrderRepository;

import java.util.List;

@Service
@RequiredArgsConstructor
public class OrderService {

    private final OrderRepository orderRepository;
    private final CartService cartService;

    @Transactional
    public Order createOrder(Long userId) {
        List<Cart> cartItems = cartService.getCart(userId);
        if (cartItems.isEmpty()) return null;

        List<OrderItem> items = cartItems.stream().map(cart -> OrderItem.builder()
                .productName(cart.getProduct().getName())
                .price(cart.getProduct().getPrice())
                .quantity(cart.getQuantity())
                .build()
        ).toList();

        double total = cartService.getTotalPrice(userId);

        Order order = Order.builder()
                .userId(userId)
                .totalPrice(total)
                .items(items)
                .build();

        Order saved = orderRepository.save(order);
        cartService.clearCart(userId);
        return saved;
    }

    public List<Order> getAllOrders() {
        return orderRepository.findAllByOrderByCreatedAtDesc();
    }

    public void updateStatus(Long orderId, OrderStatus status) {
        Order order = orderRepository.findById(orderId).orElseThrow();
        order.setStatus(status);
        orderRepository.save(order);
    }
}