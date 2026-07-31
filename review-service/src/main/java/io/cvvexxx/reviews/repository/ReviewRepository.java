package io.cvvexxx.reviews.repository;

import io.cvvexxx.reviews.dto.ReviewStatsDto;
import io.cvvexxx.reviews.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface ReviewRepository extends JpaRepository<Review, UUID> {

    Page<Review> findAllByProductId(UUID productId, Pageable pageable);

    boolean existsByProductIdAndUserId(UUID productId, UUID userId);

    @Query("""
                SELECT new io.cvvexxx.reviews.dto.ReviewStatsDto(
                    r.productId,
                    COALESCE(AVG(r.rating), 0.0),
                    COUNT(r)
                )
                FROM Review r
                WHERE r.productId = :productId
                GROUP BY r.productId
            """)
    Optional<ReviewStatsDto> getProductStats(@Param("productId") UUID productId);
}
