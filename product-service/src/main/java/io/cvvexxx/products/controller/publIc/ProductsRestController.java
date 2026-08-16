package io.cvvexxx.products.controller.publIc;


import io.cvvexxx.products.controller.payload.NewProductPayload;
import io.cvvexxx.products.dto.ProductDto;
import io.cvvexxx.products.service.product.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("api/products")
@RequiredArgsConstructor
public class ProductsRestController {

    private final ProductService productService;

    @GetMapping
    public ResponseEntity<List<ProductDto>> getAllProducts(
            @RequestParam(value = "filter", required = false) String filter
    ) {
        return ResponseEntity.ok(productService.findAllProducts(filter));
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> createProduct(
            @Valid @RequestPart("payload") NewProductPayload payload,
            @RequestPart(value = "image", required = false) MultipartFile image,
            UriComponentsBuilder uriComponentsBuilder,
            BindingResult bindingResult
    ) throws BindException {

        if (bindingResult.hasErrors()) {
            if (bindingResult instanceof BindException exception) {
                throw exception;
            }
            throw new BindException(bindingResult);
        } else {
            ProductDto createdProduct = this.productService.createProduct(
                    payload.title(),
                    payload.description(),
                    payload.quantity(),
                    payload.price(),
                    payload.createdBy(),
                    image
            );

            return ResponseEntity.created(
                            uriComponentsBuilder
                                    .replacePath("/api/products/{productId}")
                                    .build(Map.of("productId", createdProduct.id()))
                    )
                    .body(createdProduct);
        }
    }
}
