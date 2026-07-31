package io.cvvexxx.reviews.repository;

import io.cvvexxx.reviews.entity.Review;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ReviewRepository extends JpaRepository<Review, UUID> {
}
