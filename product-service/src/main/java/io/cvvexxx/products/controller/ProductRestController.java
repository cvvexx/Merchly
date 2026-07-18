package io.cvvexxx.products.controller;


import io.cvvexxx.products.controller.payload.UpdateProductPayload;
import io.cvvexxx.products.entity.Product;
import io.cvvexxx.products.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.Locale;
import java.util.NoSuchElementException;

@RestController
@RequestMapping("api/products/{productId:\\d+}")
@RequiredArgsConstructor
public class ProductRestController {

    private final ProductService productService;

    private final MessageSource messageSource;

    @ModelAttribute("product")
    public Product getProduct(@PathVariable("productId") int productId) {
        return productService.findProductById(productId)
                .orElseThrow(() -> new NoSuchElementException("catalogue.errors.product.not_found"));
    }

    @GetMapping
    public Product findProduct(@ModelAttribute("product") Product product) {
        return product;
    }

    @PatchMapping
    public ResponseEntity<?> updateProduct(
            @PathVariable("productId") int productId,
            @Valid @RequestBody UpdateProductPayload payload,
            BindingResult bindingResult
    ) throws BindException {
        if (bindingResult.hasErrors()) {
            if (bindingResult instanceof BindException exception) {
                throw exception;
            }
            throw new BindException(bindingResult);
        } else {
            productService.updateProduct(productId, payload.title(), payload.description(), payload.price());
            return ResponseEntity.noContent()
                    .build();

        }
    }

    @DeleteMapping
    public ResponseEntity<?> deleteProduct(
            @PathVariable("productId") int productId
    ) {
        productService.deleteProduct(productId);
        return ResponseEntity.noContent()
                .build();
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ProblemDetail> handleNoSuchElementException(
            NoSuchElementException exception,
            Locale locale
    ) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(
                        ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND,
                                messageSource.getMessage(exception.getMessage(), new Object[0],
                                        exception.getMessage(), locale))
                );

    }
}
