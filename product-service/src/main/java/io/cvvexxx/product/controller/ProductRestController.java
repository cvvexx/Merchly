package io.cvvexxx.product.controller;


import io.cvvexxx.product.controller.payload.UpdateProductPayload;
import io.cvvexxx.product.entity.Product;
import io.cvvexxx.product.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.NoSuchElementException;

@RestController
@RequestMapping("api/products/{productId:\\d+}")
@RequiredArgsConstructor
public class ProductRestController {

    private final ProductService productService;

    @ModelAttribute("product")
    public Product getProduct(@PathVariable("productId") int productId) {
        return productService.findProductById(productId)
                .orElseThrow(() -> new NoSuchElementException(""));//TODO
    }

    @GetMapping
    public Product findProduct(@ModelAttribute("product") Product product) {
        return product;
    }

    @PatchMapping
    public ResponseEntity<?> updateProduct(
            @PathVariable("productId") int productId,
            @RequestBody UpdateProductPayload payload
    ) {
        productService.updateProduct(productId, payload.title(), payload.description());
        return ResponseEntity.noContent()
                .build();
    }

    @DeleteMapping
    public ResponseEntity<?> deleteProduct(
            @PathVariable("productId") int productId
    ) {
        productService.deleteProduct(productId);
        return ResponseEntity.noContent()
                .build();
    }
}
