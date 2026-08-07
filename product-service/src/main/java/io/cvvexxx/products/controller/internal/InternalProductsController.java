package io.cvvexxx.products.controller.internal;


import io.cvvexxx.products.dto.ProductDto;
import io.cvvexxx.products.entity.Product;
import io.cvvexxx.products.service.ProductService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RequestMapping("api/internal/products")
@RestController
@RequiredArgsConstructor
@Slf4j
public class InternalProductsController {

    private final ProductService productService;

    @GetMapping
    @PreAuthorize("hasRole('INTERNAL_SERVICE')")
    public ResponseEntity<List<ProductDto>> findAllProducts(
            @RequestParam(required = false, name = "ids") List<UUID> ids
    ) {
        if (ids.isEmpty() || ids == null) {
            return ResponseEntity.ok(List.of());
        }

        return ResponseEntity.ok(productService.findAllByIdIn(ids));
    }

}
