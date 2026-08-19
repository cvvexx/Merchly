package io.cvvexxx.products.exception;

import lombok.Getter;
import java.util.UUID;

@Getter
public class InsufficientStockException extends RuntimeException {
    private final UUID productId;

    public InsufficientStockException(UUID productId, String message) {
        super(message);
        this.productId = productId;
    }
}