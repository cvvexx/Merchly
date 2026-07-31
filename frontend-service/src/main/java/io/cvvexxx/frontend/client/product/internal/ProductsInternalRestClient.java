package io.cvvexxx.frontend.client.product.internal;

import io.cvvexxx.frontend.dto.product.Product;

import java.util.List;
import java.util.UUID;

public interface ProductsInternalRestClient {

    List<Product> findAllProductsByIds(List<UUID> ids);

}
