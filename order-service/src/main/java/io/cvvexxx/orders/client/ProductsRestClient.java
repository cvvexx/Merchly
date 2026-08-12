package io.cvvexxx.orders.client;

import io.cvvexxx.orders.dto.ProductDto;

import java.util.List;
import java.util.UUID;

public interface ProductsRestClient {

    List<ProductDto> getProductsByIds(List<UUID> ids);

}
