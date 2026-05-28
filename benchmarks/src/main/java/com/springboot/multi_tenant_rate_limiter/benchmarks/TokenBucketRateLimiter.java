package com.springboot.multi_tenant_rate_limiter.benchmarks;

/**
 * Simple in-memory token bucket implementation for benchmarking.
 * 
 * This is a reference implementation to measure token bucket operations
 * without external dependencies.
 */
public class TokenBucketRateLimiter {
    private final long capacity;
    private final long refillRatePerSecond;
    private long tokens;
    private long lastRefillTime;

    public TokenBucketRateLimiter(long capacity, long refillPeriodSeconds) {
        this.capacity = capacity;
        this.refillRatePerSecond = capacity / refillPeriodSeconds;
        this.tokens = capacity;
        this.lastRefillTime = System.nanoTime();
    }

    public synchronized boolean tryAcquire() {
        return tryAcquire(1);
    }

    public synchronized boolean tryAcquire(long tokens) {
        refill();
        if (this.tokens >= tokens) {
            this.tokens -= tokens;
            return true;
        }
        return false;
    }

    private void refill() {
        long now = System.nanoTime();
        long timePassed = now - lastRefillTime;
        long tokensToAdd = (timePassed * refillRatePerSecond) / 1_000_000_000L;
        
        if (tokensToAdd > 0) {
            tokens = Math.min(capacity, tokens + tokensToAdd);
            lastRefillTime = now;
        }
    }

    public synchronized long getAvailableTokens() {
        refill();
        return tokens;
    }
}