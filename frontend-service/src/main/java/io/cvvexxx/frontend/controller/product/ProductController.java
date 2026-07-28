package io.cvvexxx.frontend.controller.product;

import io.cvvexxx.frontend.client.product.ProductsRestClient;
import io.cvvexxx.frontend.client.user.internal.UserInternalRestClient;
import io.cvvexxx.frontend.controller.product.payload.UpdateProductPayload;
import io.cvvexxx.frontend.dto.Product;
import io.cvvexxx.frontend.dto.ProductOwnerDto;
import io.cvvexxx.frontend.exception.BadRequestException;
import io.cvvexxx.frontend.formater.ImageUrlFormatter;
import io.cvvexxx.frontend.view.ProductOwnerViewModel;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.MessageSource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Locale;
import java.util.NoSuchElementException;

@Controller
@RequestMapping("catalogue/products/{productId:\\d+}")
@RequiredArgsConstructor
@Slf4j
public class ProductController {

    private final ProductsRestClient productsRestClient;
    private final UserInternalRestClient userInternalRestClient;
    private final MessageSource messageSource;
    private final ImageUrlFormatter imageUrlFormatter;

    @ModelAttribute("product")
    public Product product(@PathVariable("productId") int productId) {
        return productsRestClient.findProductById(productId)
                .orElseThrow(() -> new NoSuchElementException("catalogue.errors.product.not_found"));
    }

    @GetMapping
    public String getProductPage(
            @ModelAttribute("product") Product product,
            Model model
    ) {
        ProductOwnerDto user = userInternalRestClient.findUserById(product.createdBy());

        String displayImageUrl = (product.imageFileName() != null && !product.imageFileName().isBlank())
                ? imageUrlFormatter.getImageUrl(product.imageFileName())
                : "/images/default-product-image.png";

        //TODO(ПОЧИНИТЬ МАКСИМАЛЬНЫЙ РАЗМЕР ФАЙЛА)

        ProductOwnerViewModel productOwnerViewModel = new ProductOwnerViewModel(product, user, displayImageUrl);
        log.info("getProductPage product owner: {}", productOwnerViewModel);
        model.addAttribute("data", productOwnerViewModel);

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
            productsRestClient.updateProduct(
                    product.id(),
                    payload.title(),
                    payload.description(),
                    payload.price(),
                    image
            );
            return "redirect:/catalogue/products/%d".formatted(product.id());
        } catch (BadRequestException exception) {
            model.addAttribute("payload", payload);
            model.addAttribute("errors", exception.getErrors());
            return "catalogue/products/edit";
        }

    }

    @PostMapping("delete")//TODO(ПЕРЕПИСАТЬ НОРМАЛЬНО)
    public String deleteProduct(@ModelAttribute Product product) {
        productsRestClient.deleteProduct(product.id());
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

        return "errors/404";
    }

    @RequestMapping("error-403")
    public String accessDenied() {
        return "error/403";
    }
}
