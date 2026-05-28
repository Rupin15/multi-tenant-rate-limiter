package com.springboot.multi_tenant_rate_limiter.rateLimiter.tokenBucketImplementation.localRateLimiting;

import com.springboot.multi_tenant_rate_limiter.rateLimiter.tokenBucketImplementation.policy.repository.RateLimitPolicy;
import io.micrometer.core.instrument.MeterRegistry;

import java.util.concurrent.atomic.AtomicReference;

public class TokenBucket {

    private static final int MAX_CAS_RETRIES = 16;

    private final long maxTokens;
    private final long refillTokensPerSecond;

    private final AtomicReference<BucketState> state;

    private final MeterRegistry meterRegistry;

    public TokenBucket(RateLimitPolicy policy, MeterRegistry meterRegistry) {

        this.maxTokens = policy.maxTokens();
        this.refillTokensPerSecond = policy.refillTokensPerSecond();
        this.meterRegistry = meterRegistry;

        this.state = new AtomicReference<>(
                new BucketState(
                        policy.leaseSize(),
                        System.nanoTime()
                )
        );
    }

    public long getLastUpdatedTimestamp() {
        return state.get().lastRefillTimestamp();
    }

    public boolean tryConsume() {
        return tryConsumeInternal(false);
    }

    public boolean tryConsumeWithLocalRefill() {
        return tryConsumeInternal(true);
    }

    private boolean tryConsumeInternal(boolean autonomousRefill) {

        for (int retry = 0; retry < MAX_CAS_RETRIES; retry++) {

            BucketState current = state.get();

            double availableTokens = current.tokens();

            long now = System.nanoTime();

            long refillTimestamp = current.lastRefillTimestamp();

            if (autonomousRefill) {

                double elapsedSeconds =
                        (now - refillTimestamp) / 1_000_000_000D;

                double refill =
                        elapsedSeconds * refillTokensPerSecond;

                if (refill > 0D) {

                    availableTokens =
                            Math.min(
                                    maxTokens,
                                    availableTokens + refill
                            );

                    refillTimestamp = now;
                }
            }

            boolean allowed = availableTokens >= 1D;

            double remainingTokens =
                    allowed
                            ? availableTokens - 1D
                            : availableTokens;

            BucketState updated =
                    new BucketState(
                            remainingTokens,
                            refillTimestamp
                    );

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

            double updatedTokens =
                    Math.min(
                            maxTokens,
                            current.tokens() + leasedTokens
                    );

            boolean allowed = updatedTokens >= 1D;

            double remainingTokens =
                    allowed
                            ? updatedTokens - 1D
                            : updatedTokens;

            BucketState updated =
                    new BucketState(
                            remainingTokens,
                            System.nanoTime()
                    );

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

        meterRegistry
                .counter(
                        "rate_limiter.local.outcome",
                        "result",
                        allowed ? "allowed" : "rejected"
                )
                .increment();
    }

    private void recordCasRetryFailure() {

        if (meterRegistry == null) {
            return;
        }

        meterRegistry
                .counter(
                        "rate_limiter.local.cas.retry.exhausted"
                )
                .increment();
    }

    private record BucketState(
            double tokens,
            long lastRefillTimestamp
    ) {
    }
}