package io.cvvexxx.frontend.controller.product;


import io.cvvexxx.frontend.client.product.ProductsRestClient;
import io.cvvexxx.frontend.controller.product.payload.NewProductPayload;
import io.cvvexxx.frontend.dto.Product;
import io.cvvexxx.frontend.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("catalogue/products")
@RequiredArgsConstructor
public class ProductsController {

    private final ProductsRestClient restClient;

    @GetMapping("list")
    public String getProductsList(
            Model model,
            @RequestParam(name = "filter", required = false) String filter
    ) {
        model.addAttribute("products", this.restClient.findAllProducts(filter));
        model.addAttribute("filter");
        return "catalogue/products/list";
    }

    @GetMapping("create")
    public String getNewProductPage() {
        return "catalogue/products/new_product";
    }

    @PostMapping("create")
    public String createProduct(
            NewProductPayload payload,
            Model model
    ) {
        try {
            Product createdProduct =
                    restClient.createProduct(payload.title(), payload.description());

            return "redirect:/catalogue/products/%d".formatted(createdProduct.id());
        } catch (BadRequestException exception) {
            model.addAttribute("payload", payload);
            model.addAttribute("errors", exception.getErrors());
            return "catalogue/products/new_product";
        }
    }
}
