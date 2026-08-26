package io.cvvexxx.apigateway.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class FallbackController {

    @RequestMapping("/fallback/products")
    public ResponseEntity<Map<String, String>> productServiceFallback() {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of(
                        "error", "product_service_unavailable",
                        "message", "Каталог временно недоступен, попробуйте позже"
                ));
    }

    @RequestMapping("/fallback/users")
    public ResponseEntity<Map<String, String>> usersServiceFallback() {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of(
                        "error", "users_service_unavailable",
                        "message", "Профиль и корзина временно недоступны, попробуйте позже"
                ));
    }

    @RequestMapping("/fallback/orders")
    public ResponseEntity<Map<String, String>> ordersServiceFallback() {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of(
                        "error", "orders_service_unavailable",
                        "message", "Заказы временно недоступны, попробуйте позже"
                ));
    }

    @RequestMapping("/fallback/reviews")
    public ResponseEntity<Map<String, String>> reviewsServiceFallback() {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                .body(Map.of(
                        "error", "reviews_service_unavailable",
                        "message", "Отзывы временно недоступны, попробуйте позже"
                ));
    }
}
