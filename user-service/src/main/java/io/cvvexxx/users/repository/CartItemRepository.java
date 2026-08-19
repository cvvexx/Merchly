package io.cvvexxx.users.repository;

import io.cvvexxx.users.entity.CartItem;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CartItemRepository extends JpaRepository<CartItem, UUID> {

    List<CartItem> findAllByUserId(UUID userId);//TODO(СДЕЛАТЬ OPTIONAL)

    Optional<CartItem> findByUserIdAndProductId(UUID userId, UUID productId);
}
