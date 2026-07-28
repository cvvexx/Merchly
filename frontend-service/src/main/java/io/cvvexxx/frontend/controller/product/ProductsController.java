package io.cvvexxx.frontend.controller.product;


import io.cvvexxx.frontend.client.product.ProductsRestClient;
import io.cvvexxx.frontend.client.user.internal.UserInternalRestClient;
import io.cvvexxx.frontend.client.user.publIc.UserPublicRestClient;
import io.cvvexxx.frontend.controller.product.payload.NewProductPayload;
import io.cvvexxx.frontend.dto.Product;
import io.cvvexxx.frontend.dto.ProductOwnerDto;
import io.cvvexxx.frontend.exception.BadRequestException;
import io.cvvexxx.frontend.formater.ImageUrlFormatter;
import io.cvvexxx.frontend.security.KeycloakJwtAuthenticationToken;
import io.cvvexxx.frontend.view.ProductOwnerViewModel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Controller
@RequestMapping("catalogue/products")
@RequiredArgsConstructor
@Slf4j
public class ProductsController {

    private final ProductsRestClient productsRestClient;
    private final UserInternalRestClient userInternalRestClient;
    private final ImageUrlFormatter imageUrlFormatter;

    @GetMapping("list")
    public String getProductsList(
            Model model,
            @RequestParam(name = "filter", required = false) String filter
    ) {
        List<Product> products = this.productsRestClient.findAllProducts(filter);

        List<UUID> creatorIds = products.stream()
                .map(Product::createdBy)
                .distinct()
                .toList();

        List<ProductOwnerDto> creators = this.userInternalRestClient.findAllUsersByIds(creatorIds);

        Map<UUID, ProductOwnerDto> creatorsMap = creators.stream()
                .collect(Collectors.toMap(ProductOwnerDto::id, Function.identity()));

        List<ProductOwnerViewModel> viewModels = products.stream()
                .map(product -> new ProductOwnerViewModel(
                        product,
                        creatorsMap.get(product.createdBy()),
                        getImageUrl(product)
                ))
                .toList();

        model.addAttribute("products", viewModels);
        model.addAttribute("filter", filter);

        return "catalogue/products/list";
    }

    @GetMapping("create")
    public String getNewProductPage() {
        return "catalogue/products/new_product";
    }

    @PostMapping("create")
    public String createProduct(
            NewProductPayload payload,
            MultipartFile image,
            Model model,
            KeycloakJwtAuthenticationToken token
    ) {
        try {
            log.info("image {}", image);
            UUID userId = token.getUserId();

            Product createdProduct = productsRestClient.createProduct(
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

    private String getImageUrl(Product product) {
        return (product.imageFileName() != null && !product.imageFileName().isBlank())
                ? imageUrlFormatter.getImageUrl(product.imageFileName())
                : "/images/default-product-image.png";
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
