package io.cvvexxx.frontend.controller.product;

import io.cvvexxx.frontend.client.product.publIc.ProductsPublicRestClient;
import io.cvvexxx.frontend.controller.product.payload.UpdateProductPayload;
import io.cvvexxx.frontend.dto.product.Product;
import io.cvvexxx.frontend.dto.product.ProductPageData;
import io.cvvexxx.frontend.exception.BadRequestException;
import io.cvvexxx.frontend.security.KeycloakJwtAuthenticationToken;
import io.cvvexxx.frontend.service.product.DefaultProductService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.UUID;

@Controller
@RequestMapping("catalogue/products/{productId}")
@RequiredArgsConstructor
@Slf4j
public class ProductController {

    private final ProductsPublicRestClient productsPublicRestClient;
    private final MessageSource messageSource;
    private final DefaultProductService defaultProductService;

    @ModelAttribute("product")
    public Product product(@PathVariable("productId") UUID productId) {
        return productsPublicRestClient.findProductById(productId)
                .orElseThrow(() -> new NoSuchElementException("catalogue.errors.product.not_found"));
    }

    @GetMapping
    public String getProductPage(
            @ModelAttribute("product") Product product,
            @PageableDefault(size = 5) Pageable pageable,
            Model model,
            KeycloakJwtAuthenticationToken token
    ) {
        ProductPageData productPageData = defaultProductService.getProductPage(
                product,
                pageable,
                token
        );

        model.addAttribute("isAdmin", productPageData.isAdmin());
        model.addAttribute("authUsername", productPageData.authUsername());
        model.addAttribute("data", productPageData.viewModel());

        return "catalogue/products/product";
    }

    @GetMapping("edit")
    public String getProductEditPage() {
        return "catalogue/products/edit";
    }

    @PostMapping("edit")
    public String updateProduct(
            @ModelAttribute("product") Product product,
            MultipartFile image,
            UpdateProductPayload payload,
            Model model
    ) {
        try {
            productsPublicRestClient.updateProduct(
                    product.id(),
                    payload.title(),
                    payload.description(),
                    payload.price(),
                    image
            );
            return "redirect:/catalogue/products/%s".formatted(product.id());
        } catch (BadRequestException exception) {
            model.addAttribute("payload", payload);
            model.addAttribute("errors", exception.getErrors());
            return "catalogue/products/edit";
        }
    }

    @PostMapping("delete")
    public String deleteProduct(@ModelAttribute("product") Product product) {
        productsPublicRestClient.deleteProduct(product.id());
        return "redirect:/catalogue/products/list";
    }

    @ExceptionHandler(NoSuchElementException.class)
    public String handleNoSuchElementException(
            NoSuchElementException exception,
            Locale locale,
            HttpServletResponse response,
            Model model
    ) {
        response.setStatus(HttpStatus.NOT_FOUND.value());
        model.addAttribute("error",
                messageSource.getMessage(exception.getMessage(), new Object[0],
                        exception.getMessage(), locale));

        return "error/404";
    }

    @RequestMapping("error-403")
    public String accessDenied() {
        return "error/403";
    }
}