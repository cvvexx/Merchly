package io.cvvexxx.frontend.controller;

import io.cvvexxx.frontend.client.ProductsRestClient;
import io.cvvexxx.frontend.controller.payload.UpdateProductPayload;
import io.cvvexxx.frontend.entity.Product;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.NoSuchElementException;

@Controller
@RequestMapping("catalogue/products/{productId:\\d+}")
@RequiredArgsConstructor
public class ProductController {

    private final ProductsRestClient restClient;

    @ModelAttribute("product")
    public Product product(@PathVariable("productId") int productId) {
        return restClient.findProductById(productId)
                .orElseThrow(() -> new NoSuchElementException(""));//TODO("добавить страницу ошибки")
    }

    @GetMapping
    public String getProductPage() {
        return "catalogue/products/product";
    }

    @GetMapping("edit")
    public String getProductEditPage() {
        return "catalogue/products/edit";
    }

    @PostMapping("edit")
    public String updateProduct(
            @ModelAttribute("product") Product product,
            UpdateProductPayload payload,
            Model model
    ) {
        restClient.updateProduct(
                product.id(),
                payload.title(),
                payload.description()
        );
        return "redirect:/catalogue/products/%d".formatted(product.id());
        //TODO(BadRequestException)
    }

    @PostMapping("delete")//TODO(ПЕРЕПИСАТЬ НОРМАЛЬНО)
    public String deleteProduct(@ModelAttribute Product product) {
        restClient.deleteProduct(product.id());
        return "redirect:/catalogue/products/list";
    }
}
