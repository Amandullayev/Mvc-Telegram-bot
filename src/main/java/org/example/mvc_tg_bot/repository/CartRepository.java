package org.example.mvc_tg_bot.repository;

import org.example.mvc_tg_bot.model.Cart;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CartRepository extends JpaRepository<Cart, Long> {
    List<Cart> findByUserId(Long userId);
    Cart findByUserIdAndProductId(Long userId, Long productId);
    void deleteByUserId(Long userId);
}