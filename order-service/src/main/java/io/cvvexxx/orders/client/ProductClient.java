package io.cvvexxx.orders.client;

import io.cvvexxx.orders.dto.ProductDto;

import java.util.UUID;

public interface ProductClient {

    ProductDto findById(UUID productId);
}
