package io.cvvexxx.frontend.service.product;

import io.cvvexxx.frontend.dto.product.Product;
import io.cvvexxx.frontend.dto.product.ProductListData;
import io.cvvexxx.frontend.dto.product.ProductPageData;
import io.cvvexxx.frontend.security.KeycloakJwtAuthenticationToken;
import org.springframework.data.domain.Pageable;

public interface ProductService {

    ProductListData getProductsList(String filter);

    ProductPageData getProductPage(Product product, Pageable pageable, KeycloakJwtAuthenticationToken token);

}
