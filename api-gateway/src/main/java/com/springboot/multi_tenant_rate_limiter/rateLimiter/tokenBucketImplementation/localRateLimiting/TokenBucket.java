package com.springboot.multi_tenant_rate_limiter.rateLimiter.tokenBucketImplementation.localRateLimiting;

import io.micrometer.core.instrument.MeterRegistry;

import java.util.concurrent.atomic.AtomicReference;

public class TokenBucket {

    private static final int MAX_CAS_RETRIES = 16;

    private final long maxTokens;

    private final AtomicReference<BucketState> state;

    private final MeterRegistry meterRegistry;

    public TokenBucket( long maxTokens,MeterRegistry meterRegistry) {
        this.maxTokens = maxTokens;
        this.meterRegistry = meterRegistry;
        this.state = new AtomicReference<>(new BucketState(0D, System.nanoTime()));
    }

    public long getLastUpdatedTimestamp() {
        return state.get().lastUpdatedTimestamp();
    }

    public boolean tryConsume() {

        for (int retry = 0; retry < MAX_CAS_RETRIES; retry++) {
            BucketState current = state.get();
            boolean allowed = current.tokens() >= 1D;
            double remainingTokens =allowed ? current.tokens() - 1D : current.tokens();
            BucketState updated = new BucketState(remainingTokens,System.nanoTime());
            if (state.compareAndSet(current, updated)) {
                recordOutcome(allowed);
                return allowed;
            }
            Thread.onSpinWait();
        }
        recordCasRetryFailure();
        return false;
    }

    public boolean addTokensAndConsume(long leasedTokens) {
        if (leasedTokens <= 0) {
            return false;
        }
        for (int retry = 0; retry < MAX_CAS_RETRIES; retry++) {
            BucketState current = state.get();
            double updatedTokens = Math.min( maxTokens, current.tokens() + leasedTokens);
            boolean allowed = updatedTokens >= 1D;
            double remainingTokens = allowed? updatedTokens - 1D : updatedTokens;
            BucketState updated = new BucketState(remainingTokens, System.nanoTime());
            if (state.compareAndSet(current, updated)) {
                recordOutcome(allowed);
                return allowed;
            }
            Thread.onSpinWait();
        }
        recordCasRetryFailure();
        return false;
    }

    private void recordOutcome(boolean allowed) {

        if (meterRegistry == null) {
            return;
        }

        meterRegistry.counter(
                "rate_limiter.local.outcome",
                "result",
                allowed ? "allowed" : "rejected"
        ).increment();
    }

    private void recordCasRetryFailure() {

        if (meterRegistry == null) {
            return;
        }

        meterRegistry.counter(
                "rate_limiter.local.cas.retry.exhausted"
        ).increment();
    }

    private record BucketState(
            double tokens,
            long lastUpdatedTimestamp
    ) {
    }
}