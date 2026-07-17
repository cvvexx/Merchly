package io.cvvexxx.products.controller;


import io.cvvexxx.products.controller.payload.NewProductPayload;
import io.cvvexxx.products.entity.Product;
import io.cvvexxx.products.service.ProductService;
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
    public List<Product> getAllProducts(
            @RequestParam(value = "filter", required = false) String filter
    ) {
        return productService.findAllProducts(filter);
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
