CREATE TABLE processed_order_events
(
    order_id     UUID PRIMARY KEY,
    processed_at TIMESTAMP NOT NULL
);
