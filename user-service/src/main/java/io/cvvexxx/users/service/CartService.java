package io.cvvexxx.users.service;

import io.cvvexxx.users.dto.cart.AddToCartDto;
import io.cvvexxx.users.dto.cart.CartItemDto;
import io.cvvexxx.users.entity.CartItem;
import io.cvvexxx.users.repository.CartItemRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class CartService {

    private final CartItemRepository cartItemRepository;

    @Transactional
    public void addItemToCart(AddToCartDto addToCartDto, UUID currentUserId) {

        Optional<CartItem> byUserIdAndProductId =
                cartItemRepository.findByUserIdAndProductId(currentUserId, addToCartDto.productId());
        log.info("finding product by userId {} and productId {}", currentUserId,  addToCartDto.productId());
        byUserIdAndProductId
                .ifPresentOrElse(
                        cartItem -> {
                            log.info("Adding to cart item {}", cartItem);
                            cartItem.setQuantity(cartItem.getQuantity() + 1);
                        },
                        () -> {
                            var saved = cartItemRepository.save(
                                    CartItem.builder()
                                            .id(UUID.randomUUID())
                                            .userId(currentUserId)
                                            .productId(addToCartDto.productId())
                                            .quantity(addToCartDto.quantity())
                                            .build());
                            log.info("Saved cart item {}", saved);
                        }
                        );
    }

    @Transactional
    public List<CartItemDto> getCartItems(UUID currentUserId) {
        return cartItemRepository.findAllByUserId(currentUserId)
                .stream()
                .map(cartItem -> new CartItemDto(
                        cartItem.getProductId(),
                        cartItem.getQuantity()
                ))
                .toList();
    }
}
