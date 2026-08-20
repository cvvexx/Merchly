package io.cvvexxx.products.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "processed_order_events")
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class ProcessedOrderEvent {

    @Id
    private UUID orderId;

    private Instant processedAt;
}
