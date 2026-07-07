package io.cvvexxx.backend.controller;


import io.cvvexxx.backend.controller.payload.NewProductPayload;
import io.cvvexxx.backend.entity.Product;
import io.cvvexxx.backend.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("api/products")
@RequiredArgsConstructor
public class ProductsRestController {

    private final ProductService productService;

    @GetMapping
    public List<Product> getAllProducts() {
        return productService.findAllProducts();
    }

    @PostMapping
    public ResponseEntity<?> createProduct(
            @RequestBody NewProductPayload payload,
            UriComponentsBuilder uriComponentsBuilder
    ) {
        Product createdProduct = this.productService.createProduct(
                payload.title(),
                payload.description()
        );

        return ResponseEntity.created(
                        uriComponentsBuilder
                                .replacePath("/api/products/{productId}")
                                .build(Map.of("productId", createdProduct.getId()))
                )
                .body(createdProduct);
    }
}
