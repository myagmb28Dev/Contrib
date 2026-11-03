package com.example.project.common.ratelimit;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.Test;

class RateLimiterTest {

    static class MutableClock extends Clock {
        private final AtomicLong millis;

        MutableClock(long initialMillis) {
            this.millis = new AtomicLong(initialMillis);
        }

        void advance(long amountMillis) {
            millis.addAndGet(amountMillis);
        }

        @Override
        public ZoneOffset getZone() {
            return ZoneOffset.UTC;
        }

        @Override
        public Clock withZone(java.time.ZoneId zone) {
            return this;
        }

        @Override
        public Instant instant() {
            return Instant.ofEpochMilli(millis.get());
        }

        @Override
        public long millis() {
            return millis.get();
        }
    }

    @Test
    void allowsRequestsUpToCapacity() {
        MutableClock clock = new MutableClock(1_000_000L);
        RateLimiter limiter = new RateLimiter(clock);

        for (int i = 0; i < 5; i++) {
            assertThat(limiter.tryAcquire("test-key", 5, 60)).isTrue();
        }
        assertThat(limiter.tryAcquire("test-key", 5, 60)).isFalse();
    }

    @Test
    void refillsTokensOverTime() {
        MutableClock clock = new MutableClock(1_000_000L);
        RateLimiter limiter = new RateLimiter(clock);

        for (int i = 0; i < 2; i++) {
            assertThat(limiter.tryAcquire("refill-key", 2, 60)).isTrue();
        }
        assertThat(limiter.tryAcquire("refill-key", 2, 60)).isFalse();

        clock.advance(30_000L);
        assertThat(limiter.tryAcquire("refill-key", 2, 60)).isTrue();
        assertThat(limiter.tryAcquire("refill-key", 2, 60)).isFalse();

        clock.advance(60_000L);
        assertThat(limiter.tryAcquire("refill-key", 2, 60)).isTrue();
        assertThat(limiter.tryAcquire("refill-key", 2, 60)).isTrue();
        assertThat(limiter.tryAcquire("refill-key", 2, 60)).isFalse();
    }

    @Test
    void separatesDifferentKeys() {
        MutableClock clock = new MutableClock(1_000_000L);
        RateLimiter limiter = new RateLimiter(clock);

        assertThat(limiter.tryAcquire("user-1", 1, 60)).isTrue();
        assertThat(limiter.tryAcquire("user-1", 1, 60)).isFalse();
        assertThat(limiter.tryAcquire("user-2", 1, 60)).isTrue();
    }

    @Test
    void rejectsWhenCapacityIsZeroOrNegative() {
        RateLimiter limiter = new RateLimiter();
        assertThat(limiter.tryAcquire("key", 0, 60)).isFalse();
        assertThat(limiter.tryAcquire("key", -1, 60)).isFalse();
    }

    @Test
    void resetClearsAllBuckets() {
        MutableClock clock = new MutableClock(1_000_000L);
        RateLimiter limiter = new RateLimiter(clock);

        assertThat(limiter.tryAcquire("key", 1, 60)).isTrue();
        assertThat(limiter.tryAcquire("key", 1, 60)).isFalse();

        limiter.reset();
        assertThat(limiter.tryAcquire("key", 1, 60)).isTrue();
    }
}
