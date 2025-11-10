package org.example.mvc_tg_bot.service;

import lombok.RequiredArgsConstructor;
import org.example.mvc_tg_bot.model.Cart;
import org.example.mvc_tg_bot.model.Product;
import org.example.mvc_tg_bot.repository.CartRepository;
import org.springframework.stereotype.Service;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CartService {
    private final CartRepository cartRepository;
    private final ProductService productService;

    public void addToCart(Long userId, Long productId) {
        Product product = productService.findById(productId);
        if (product == null || !product.isActive()) return;

        Cart existing = cartRepository.findByUserIdAndProductId(userId, productId);
        if (existing != null) {
            existing.setQuantity(existing.getQuantity() + 1);
            cartRepository.save(existing);
        } else {
            Cart cart = Cart.builder()
                    .userId(userId)
                    .product(product)
                    .quantity(1)
                    .build();
            cartRepository.save(cart);
        }
    }

    public List<Cart> getCart(Long userId) {
        return cartRepository.findByUserId(userId);
    }

    public double getTotalPrice(Long userId) {
        return getCart(userId).stream()
                .mapToDouble(c -> c.getProduct().getPrice() * c.getQuantity())
                .sum();
    }

    public void clearCart(Long userId) {
        cartRepository.deleteByUserId(userId);
    }
}