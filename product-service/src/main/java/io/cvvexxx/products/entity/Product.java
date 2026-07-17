package io.cvvexxx.products.entity;


import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Data
@NamedQueries(
        @NamedQuery(
                name = "Product.findAllByTitleLikeIgnoringCase",
                query = "select p from Product p where p.title ilike :filter"
        )
)
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Integer id;

    @NotNull
    @Size(min = 3, max = 50)
    private String title;

    @Size(max = 1000)
    private String description;

}
