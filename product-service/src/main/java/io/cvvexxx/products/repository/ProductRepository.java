package io.cvvexxx.products.repository;

import io.cvvexxx.products.entity.Product;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ProductRepository extends JpaRepository<Product, Integer> {

    @Query(name = "Product.findAllByTitleIgnoringCase")
    List<Product> findAllByTitleLikeIgnoreCase(@Param("filter") String filter);

}
