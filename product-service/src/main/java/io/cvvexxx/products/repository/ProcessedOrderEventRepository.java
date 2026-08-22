package io.cvvexxx.products.repository;

import io.cvvexxx.products.entity.ProcessedOrderEvent;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ProcessedOrderEventRepository extends JpaRepository<ProcessedOrderEvent, UUID> {
}
