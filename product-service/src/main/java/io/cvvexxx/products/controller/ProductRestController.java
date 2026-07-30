package io.cvvexxx.products.controller;

import io.cvvexxx.products.controller.payload.UpdateProductPayload;
import io.cvvexxx.products.entity.Product;
import io.cvvexxx.products.service.ProductService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindException;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.UUID;

@RestController
@RequestMapping("api/products/{productId}")
@RequiredArgsConstructor
@Slf4j
public class ProductRestController {

    private final ProductService productService;
    private final MessageSource messageSource;

    @GetMapping
    public Product findProduct(@PathVariable("productId") UUID productId) {
        // Запрос сразу идет в сервис.
        // Если в кэше Redis есть данные — сервис отдает их.
        // Если нет в БД — сервис бросает NoSuchElementException.
        return productService.findProductById(productId);
    }

    @PatchMapping
    public ResponseEntity<Void> updateProduct(
            @PathVariable("productId") UUID productId,
            @Valid @RequestPart("payload") UpdateProductPayload payload,
            @RequestPart(value = "image", required = false ) MultipartFile image,
            BindingResult bindingResult
    ) throws BindException {
        if (bindingResult.hasErrors()) {
            if (bindingResult instanceof BindException exception) {
                throw exception;
            }
            throw new BindException(bindingResult);
        }
        log.info("Updating product with id {}", productId);
        productService.updateProduct(productId, payload.title(), payload.description(), payload.price(), image);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping
    public ResponseEntity<Void> deleteProduct(
            @PathVariable("productId") UUID productId
    ) {
        productService.deleteProduct(productId);
        return ResponseEntity.noContent().build();
    }

    @ExceptionHandler(NoSuchElementException.class)
    public ResponseEntity<ProblemDetail> handleNoSuchElementException(
            NoSuchElementException exception,
            Locale locale
    ) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(
                        ProblemDetail.forStatusAndDetail(
                                HttpStatus.NOT_FOUND,
                                messageSource.getMessage(
                                        exception.getMessage(),
                                        new Object[0],
                                        exception.getMessage(),
                                        locale
                                )
                        )
                );
    }
}