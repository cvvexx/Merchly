package io.cvvexxx.orders.controller;

import io.cvvexxx.orders.domain.OrderStatus;
import io.cvvexxx.orders.dto.NewOrderDto;
import io.cvvexxx.orders.dto.NewOrderItemDto;
import io.cvvexxx.orders.dto.OrderDto;
import io.cvvexxx.orders.service.OrderService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.BindException;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderControllerTest {

    @Mock
    private OrderService orderService;

    @InjectMocks
    private OrderController controller;

    @Test
    @DisplayName("createOrder: при ошибках валидации выбрасывает BindException и не создаёт заказ")
    void createOrder_WhenBindingResultHasErrors_ShouldThrowBindException() {
        // given
        NewOrderDto dto = new NewOrderDto(List.of(new NewOrderItemDto(UUID.randomUUID(), 1)), "", "");
        var bindingResult = new BeanPropertyBindingResult(dto, "newOrderDto");
        bindingResult.reject("deliveryAddress", "must not be blank");
        Jwt jwt = jwtWithRoles(UUID.randomUUID(), List.of());

        // when / then
        assertThrows(BindException.class, () ->
                controller.createOrder(dto, bindingResult, jwt, UriComponentsBuilder.newInstance())
        );

        verifyNoInteractions(orderService);
    }

    @Test
    @DisplayName("createOrder: при валидном запросе создаёт заказ для пользователя из sub-claim и возвращает Location")
    void createOrder_WhenValid_ShouldCreateOrderForCurrentUserAndReturnLocation() throws BindException {
        // given
        UUID userId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        NewOrderDto dto = new NewOrderDto(List.of(new NewOrderItemDto(UUID.randomUUID(), 1)), "address", "comment");
        var bindingResult = new BeanPropertyBindingResult(dto, "newOrderDto");
        Jwt jwt = jwtWithRoles(userId, List.of());
        when(orderService.createOrder(dto, userId)).thenReturn(order(orderId, userId));

        // when
        ResponseEntity<OrderDto> response = controller.createOrder(
                dto, bindingResult, jwt, UriComponentsBuilder.fromUriString("http://localhost")
        );

        // then
        assertEquals(orderId, response.getBody().id());
        verify(orderService).createOrder(eq(dto), eq(userId));
    }

    private Jwt jwtWithRoles(UUID userId, List<String> roles) {
        return Jwt.withTokenValue("token")
                .header("alg", "none")
                .claim("sub", userId.toString())
                .claim("realm_access", Map.of("roles", roles))
                .build();
    }

    private OrderDto order(UUID orderId, UUID userId) {
        return new OrderDto(
                orderId, userId, OrderStatus.CONFIRMED, new BigDecimal("100.00"),
                "address", "comment", null, List.of(), Instant.now(), Instant.now()
        );
    }

    @Nested
    @DisplayName("определение роли ADMIN из JWT")
    class AdminRoleDetectionTests {

        @Test
        @DisplayName("realm_access.roles содержит 'ADMIN' -> isAdmin=true")
        void confirmOrder_WhenRealmRolesContainAdmin_ShouldPassIsAdminTrue() {
            // given
            UUID userId = UUID.randomUUID();
            UUID orderId = UUID.randomUUID();
            Jwt jwt = jwtWithRoles(userId, List.of("ADMIN"));
            when(orderService.confirmOrder(orderId, userId, true)).thenReturn(order(orderId, userId));

            // when
            controller.confirmOrder(orderId, jwt);

            // then
            verify(orderService).confirmOrder(orderId, userId, true);
        }

        @Test
        @DisplayName("realm_access.roles содержит 'ROLE_ADMIN' -> isAdmin=true")
        void confirmOrder_WhenRealmRolesContainRolePrefixedAdmin_ShouldPassIsAdminTrue() {
            // given
            UUID userId = UUID.randomUUID();
            UUID orderId = UUID.randomUUID();
            Jwt jwt = jwtWithRoles(userId, List.of("ROLE_ADMIN"));
            when(orderService.confirmOrder(orderId, userId, true)).thenReturn(order(orderId, userId));

            // when
            controller.confirmOrder(orderId, jwt);

            // then
            verify(orderService).confirmOrder(orderId, userId, true);
        }

        @Test
        @DisplayName("realm_access.roles без ADMIN -> isAdmin=false")
        void confirmOrder_WhenRealmRolesDoNotContainAdmin_ShouldPassIsAdminFalse() {
            // given
            UUID userId = UUID.randomUUID();
            UUID orderId = UUID.randomUUID();
            Jwt jwt = jwtWithRoles(userId, List.of("USER"));
            when(orderService.confirmOrder(orderId, userId, false)).thenReturn(order(orderId, userId));

            // when
            controller.confirmOrder(orderId, jwt);

            // then
            verify(orderService).confirmOrder(orderId, userId, false);
        }

        @Test
        @DisplayName("realm_access отсутствует в токене -> isAdmin=false, а не падение")
        void confirmOrder_WhenRealmAccessClaimMissing_ShouldPassIsAdminFalse() {
            // given
            UUID userId = UUID.randomUUID();
            UUID orderId = UUID.randomUUID();
            Jwt jwt = Jwt.withTokenValue("token")
                    .header("alg", "none")
                    .claim("sub", userId.toString())
                    .build();
            when(orderService.confirmOrder(orderId, userId, false)).thenReturn(order(orderId, userId));

            // when
            controller.confirmOrder(orderId, jwt);

            // then
            verify(orderService).confirmOrder(orderId, userId, false);
        }
    }
}
