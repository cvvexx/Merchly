package io.cvvexxx.frontend.dto.review;

import java.io.Serializable;
import java.util.UUID;

public record NewReviewDto(
        UUID productId,
        int rating,
        String comment

) implements Serializable {
}
