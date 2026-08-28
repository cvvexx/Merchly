package io.cvvexxx.apigateway.security.ratelimit;

import io.github.bucket4j.Bandwidth;
import io.github.bucket4j.Bucket;
import io.github.bucket4j.ConsumptionProbe;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.Supplier;

public class RateLimitBuckets {

    private static final int SWEEP_THRESHOLD = 10_000;
    private static final Duration SWEEP_INTERVAL = Duration.ofMinutes(1);

    private final Map<String, Entry> buckets = new ConcurrentHashMap<>();
    private final AtomicLong nextSweepAt = new AtomicLong();
    private final Supplier<Bucket> bucketFactory;
    private final Duration idleRetention;

    public RateLimitBuckets(int capacity, Duration window) {
        this.bucketFactory = () -> Bucket.builder()
                .addLimit(Bandwidth.builder()
                        .capacity(capacity)
                        .refillIntervally(capacity, window)
                        .build())
                .build();
        this.idleRetention = window.multipliedBy(2);
    }

    public ConsumptionProbe tryConsume(String key) {
        Entry entry = buckets.compute(key, (ignored, existing) -> {
            Entry target = existing != null ? existing : new Entry(bucketFactory.get());
            target.touch();
            return target;
        });

        sweepIfNeeded();
        return entry.bucket().tryConsumeAndReturnRemaining(1);
    }

    private void sweepIfNeeded() {
        if (buckets.size() <= SWEEP_THRESHOLD) {
            return;
        }

        long now = System.nanoTime();
        long scheduled = nextSweepAt.get();
        if (now - scheduled < 0 || !nextSweepAt.compareAndSet(scheduled, now + SWEEP_INTERVAL.toNanos())) {
            return;
        }

        long deadline = now - idleRetention.toNanos();
        buckets.entrySet().removeIf(entry -> entry.getValue().lastAccessNanos() - deadline < 0);
    }

    private static final class Entry {

        private final Bucket bucket;
        private volatile long lastAccessNanos;

        private Entry(Bucket bucket) {
            this.bucket = bucket;
        }

        private Bucket bucket() {
            return bucket;
        }

        private long lastAccessNanos() {
            return lastAccessNanos;
        }

        private void touch() {
            lastAccessNanos = System.nanoTime();
        }
    }
}
