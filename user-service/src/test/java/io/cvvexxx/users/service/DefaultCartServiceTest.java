package io.cvvexxx.users.service;

import io.cvvexxx.users.dto.cart.AddToCartDto;
import io.cvvexxx.users.dto.cart.CartItemDto;
import io.cvvexxx.users.entity.CartItem;
import io.cvvexxx.users.repository.CartItemRepository;
import io.cvvexxx.users.service.cart.DefaultCartService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DefaultCartServiceTest {

    private final UUID USER_ID = UUID.randomUUID();
    private final UUID PRODUCT_ID = UUID.randomUUID();
    @Mock
    private CartItemRepository cartItemRepository;
    @InjectMocks
    private DefaultCartService defaultCartService;

    @Nested
    @DisplayName("Тесты метода addItemToCart")
    class AddItemToCartTests {

        @Test
        @DisplayName("addItemToCart: если товар уже есть в корзине, инкрементирует количество на 1")
        void addItemToCart_WhenItemExists_ShouldIncrementQuantity() {
            // given
            AddToCartDto addToCartDto = new AddToCartDto(PRODUCT_ID, 5);

            CartItem existingCartItem = CartItem.builder()
                    .id(UUID.randomUUID())
                    .userId(USER_ID)
                    .productId(PRODUCT_ID)
                    .quantity(2)
                    .build();

            when(cartItemRepository.findByUserIdAndProductId(USER_ID, PRODUCT_ID))
                    .thenReturn(Optional.of(existingCartItem));

            // when
            defaultCartService.addItemToCart(addToCartDto, USER_ID);

            // then
            assertEquals(3, existingCartItem.getQuantity());

            verify(cartItemRepository, times(1)).findByUserIdAndProductId(USER_ID, PRODUCT_ID);
            verify(cartItemRepository, never()).save(any());
        }

        @Test
        @DisplayName("addItemToCart: если товара нет в корзине, создает и сохраняет новую запись")
        void addItemToCart_WhenItemDoesNotExist_ShouldSaveNewCartItem() {
            // given
            AddToCartDto addToCartDto = new AddToCartDto(PRODUCT_ID, 3);

            when(cartItemRepository.findByUserIdAndProductId(USER_ID, PRODUCT_ID))
                    .thenReturn(Optional.empty());

            CartItem savedItem = CartItem.builder()
                    .id(UUID.randomUUID())
                    .userId(USER_ID)
                    .productId(PRODUCT_ID)
                    .quantity(3)
                    .build();

            when(cartItemRepository.save(any(CartItem.class))).thenReturn(savedItem);

            // when
            defaultCartService.addItemToCart(addToCartDto, USER_ID);

            // then
            ArgumentCaptor<CartItem> captor = ArgumentCaptor.forClass(CartItem.class);
            verify(cartItemRepository, times(1)).save(captor.capture());

            CartItem capturedCartItem = captor.getValue();
            assertNotNull(capturedCartItem.getId());
            assertEquals(USER_ID, capturedCartItem.getUserId());
            assertEquals(PRODUCT_ID, capturedCartItem.getProductId());
            assertEquals(3, capturedCartItem.getQuantity());
        }
    }

    @Nested
    @DisplayName("Тесты метода getCartItems")
    class GetCartItemsTests {

        @Test
        @DisplayName("getCartItems: возвращает список CartItemDto для текущего пользователя")
        void getCartItems_WhenItemsExist_ShouldReturnDtoList() {
            // given
            UUID productId1 = UUID.randomUUID();
            UUID productId2 = UUID.randomUUID();

            CartItem item1 = CartItem.builder()
                    .id(UUID.randomUUID())
                    .userId(USER_ID)
                    .productId(productId1)
                    .quantity(2)
                    .build();

            CartItem item2 = CartItem.builder()
                    .id(UUID.randomUUID())
                    .userId(USER_ID)
                    .productId(productId2)
                    .quantity(5)
                    .build();

            when(cartItemRepository.findAllByUserId(USER_ID))
                    .thenReturn(List.of(item1, item2));

            // when
            List<CartItemDto> result = defaultCartService.getCartItems(USER_ID);

            // then
            assertNotNull(result);
            assertEquals(2, result.size());

            assertEquals(productId1, result.get(0).productId());
            assertEquals(2, result.get(0).quantity());

            assertEquals(productId2, result.get(1).productId());
            assertEquals(5, result.get(1).quantity());

            verify(cartItemRepository, times(1)).findAllByUserId(USER_ID);
        }

        @Test
        @DisplayName("getCartItems: если корзина пуста, возвращает пустой список")
        void getCartItems_WhenCartIsEmpty_ShouldReturnEmptyList() {
            // given
            when(cartItemRepository.findAllByUserId(USER_ID))
                    .thenReturn(List.of());

            // when
            List<CartItemDto> result = defaultCartService.getCartItems(USER_ID);

            // then
            assertNotNull(result);
            assertTrue(result.isEmpty());

            verify(cartItemRepository, times(1)).findAllByUserId(USER_ID);
        }
    }

    @Nested
    @DisplayName("Тесты метода deleterItemFromCart")
    class DeleteItemFromCartTests {

        @Test
        @DisplayName("deleterItemFromCart: успешно удаляет существующий товар из корзины")
        void deleterItemFromCart_WhenItemExists_ShouldDeleteCartItem() {
            // given
            CartItem existingItem = CartItem.builder()
                    .id(UUID.randomUUID())
                    .userId(USER_ID)
                    .productId(PRODUCT_ID)
                    .quantity(1)
                    .build();

            when(cartItemRepository.findByUserIdAndProductId(USER_ID, PRODUCT_ID))
                    .thenReturn(Optional.of(existingItem));

            // when
            defaultCartService.deleterItemFromCart(PRODUCT_ID, USER_ID);

            // then
            verify(cartItemRepository, times(1)).findByUserIdAndProductId(USER_ID, PRODUCT_ID);
            verify(cartItemRepository, times(1)).delete(existingItem);
        }

        @Test
        @DisplayName("deleterItemFromCart: выбрасывает NoSuchElementException, если товар не найден в корзине")
        void deleterItemFromCart_WhenItemNotFound_ShouldThrowNoSuchElementException() {
            // given
            when(cartItemRepository.findByUserIdAndProductId(USER_ID, PRODUCT_ID))
                    .thenReturn(Optional.empty());

            // when & then
            NoSuchElementException exception = assertThrows(
                    NoSuchElementException.class,
                    () -> defaultCartService.deleterItemFromCart(PRODUCT_ID, USER_ID)
            );

            assertEquals("errors.404.header", exception.getMessage());
            verify(cartItemRepository, times(1)).findByUserIdAndProductId(USER_ID, PRODUCT_ID);
            verify(cartItemRepository, never()).delete(any());
        }
    }
}