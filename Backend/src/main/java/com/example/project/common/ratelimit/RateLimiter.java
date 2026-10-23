package com.example.project.common.ratelimit;

import java.time.Clock;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import org.springframework.stereotype.Component;

@Component
public class RateLimiter {

    private final Clock clock;
    private final ConcurrentMap<String, TokenBucket> buckets = new ConcurrentHashMap<>();

    public RateLimiter() {
        this(Clock.systemUTC());
    }

    public RateLimiter(Clock clock) {
        this.clock = clock;
    }

    public boolean tryAcquire(String key, int capacity, long windowSeconds) {
        if (capacity <= 0) {
            return false;
        }
        long nowMillis = clock.millis();
        long windowMillis = Math.max(1000L, windowSeconds * 1000L);
        TokenBucket bucket = buckets.computeIfAbsent(key, k -> new TokenBucket(capacity, windowMillis, nowMillis));
        return bucket.tryConsume(capacity, windowMillis, nowMillis);
    }

    public void reset() {
        buckets.clear();
    }

    private static class TokenBucket {
        private final Object lock = new Object();
        private double tokens;
        private long lastRefillMillis;

        TokenBucket(int capacity, long windowMillis, long nowMillis) {
            this.tokens = capacity;
            this.lastRefillMillis = nowMillis;
        }

        boolean tryConsume(int capacity, long windowMillis, long nowMillis) {
            synchronized (lock) {
                long elapsed = Math.max(0, nowMillis - lastRefillMillis);
                double refill = (double) elapsed * capacity / windowMillis;
                tokens = Math.min(capacity, tokens + refill);
                lastRefillMillis = nowMillis;

                if (tokens >= 1.0) {
                    tokens -= 1.0;
                    return true;
                }
                return false;
            }
        }
    }
}
