package io.cvvexxx.products.entity;


import jakarta.persistence.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.SQLRestriction;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@AllArgsConstructor
@NoArgsConstructor
@Data
@Builder
@SQLRestriction("is_available = true")
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

    @Column(name = "price", nullable = false)
    private BigDecimal price;

    @Column(name = "created_by",  nullable = false)
    private Integer createdBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at",  nullable = false)
    private Instant updatedAt;

    @Builder.Default
    @Column(name = "is_available",  nullable = false)
    private boolean isAvailable = true;
}
