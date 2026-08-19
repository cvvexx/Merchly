package io.cvvexxx.products.exception;

import lombok.Getter;

import java.util.List;
import java.util.UUID;

@Getter
public class InsufficientStockException extends RuntimeException {
    private final List<UUID> productIds;

    public InsufficientStockException(List<UUID> productIds, String message) {
        super(message);
        this.productIds = productIds;
    }
}