package io.cvvexxx.reviews.dto;

import jakarta.validation.constraints.*;

import java.util.UUID;

public record NewReviewDto(//TODO(ДОБАВИТЬ СООБЩЕНИЯ ОБ ОШИБКАХ ВАЛИДАЦИИ)
        @NotNull
        UUID productId,

        @NotNull
        @Min(1)
        @Max(5)
        int rating,

        @Size(max = 2000)
        String comment
) {
}
