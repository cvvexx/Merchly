package io.cvvexxx.frontend.dto.product;

import java.math.BigDecimal;
import java.util.UUID;

public record Product(
        UUID id,
        String title,
        String description,
        BigDecimal price,
        String imageFileName,
        UUID createdBy
) {


}
