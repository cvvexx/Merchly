package io.cvvexxx.products.repository;

import io.cvvexxx.products.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Integer> {

    List<Product> findAllByTitleContainingIgnoreCase(String title);

}
