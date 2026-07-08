package io.cvvexxx.product.controller;


import io.cvvexxx.product.controller.payload.NewProductPayload;
import io.cvvexxx.product.entity.Product;
import io.cvvexxx.product.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
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
            @Valid @RequestBody NewProductPayload payload,
            UriComponentsBuilder uriComponentsBuilder,
            BindingResult bindingResult
    ) throws BindException {

        if (bindingResult.hasErrors()) {
            if (bindingResult instanceof BindException exception) {
                throw exception;
            }
            throw new BindException(bindingResult);
        } else {
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
}
