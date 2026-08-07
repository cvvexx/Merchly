package io.cvvexxx.frontend.controller.product;


import io.cvvexxx.frontend.client.product.publIc.ProductsPublicRestClient;
import io.cvvexxx.frontend.controller.product.payload.NewProductPayload;
import io.cvvexxx.frontend.dto.product.Product;
import io.cvvexxx.frontend.exception.BadRequestException;
import io.cvvexxx.frontend.security.KeycloakJwtAuthenticationToken;
import io.cvvexxx.frontend.service.product.DefaultProductService;
import io.cvvexxx.frontend.view.ProductOwnerViewModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.UUID;

@Controller
@RequestMapping("/catalogue/products")
@RequiredArgsConstructor
@Slf4j
public class ProductsController {

    private final ProductsPublicRestClient productsPublicRestClient;
    private final DefaultProductService defaultProductService;

    @GetMapping("/list")
    public String getProductsList(
            Model model,
            @RequestParam(name = "filter", required = false) String filter
    ) {
        List<ProductOwnerViewModel> viewModels =
                defaultProductService.getProductsList(filter).viewModels();

        log.info("viewModels: {}", viewModels);
        model.addAttribute("products", viewModels);
        model.addAttribute("filter", filter);

        return "catalogue/products/list";
    }

    @GetMapping("/create")
    public String getNewProductPage() {
        return "catalogue/products/new_product";
    }

    @PostMapping("/create")
    public String createProduct(
            NewProductPayload payload,
            MultipartFile image,
            Model model,
            KeycloakJwtAuthenticationToken token
    ) {
        try {
            log.info("image {}", image);
            UUID userId = token.getUserId();

            Product createdProduct = productsPublicRestClient.createProduct(
                    payload.title(),
                    payload.description(),
                    payload.price(),
                    image,
                    userId
            );

            return "redirect:/catalogue/products/%s".formatted(createdProduct.id());
        } catch (BadRequestException exception) {
            model.addAttribute("payload", payload);
            model.addAttribute("errors", exception.getErrors());
            return "catalogue/products/new_product";
        }
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public String handleMaxUploadSizeExceededException(
            MaxUploadSizeExceededException e,
            Model model
    ) {
        log.warn("Попытка загрузки слишком большого файла: {}", e.getMessage());
        model.addAttribute("errors", List.of("Размер загружаемого файла не должен превышать 10 МБ."));
        return "catalogue/products/new_product";
    }
}
