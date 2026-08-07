package io.cvvexxx.products.repository;

import io.cvvexxx.products.dto.ProductDto;
import io.cvvexxx.products.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface ProductRepository extends JpaRepository<Product, UUID> {

    List<ProductDto> findAllByTitleContainingIgnoreCase(String title);

}
