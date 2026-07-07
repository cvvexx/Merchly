package io.cvvexxx.frontend.controller;


import io.cvvexxx.frontend.client.ProductsRestClient;
import io.cvvexxx.frontend.controller.payload.NewProductPayload;
import io.cvvexxx.frontend.entity.Product;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("catalogue/products")
@RequiredArgsConstructor
public class ProductsController {

    private final ProductsRestClient restClient;

    @GetMapping("list")
    public String getProductsList(
            Model model
    ) {
        model.addAttribute("products", this.restClient.findAllProducts());
        return "catalogue/products/list";
    }

    @GetMapping("create")
    public String getNewProductPage() {
        return "catalogue/products/new_product";
    }

    @PostMapping("create")
    public String createProduct(
            NewProductPayload payload
    ) {

        Product createdProduct =
                restClient.createProduct(payload.title(), payload.description());

        return "redirect:/catalogue/products/%d".formatted(createdProduct.id());
        //TODO(BadRequestException)
    }
}
