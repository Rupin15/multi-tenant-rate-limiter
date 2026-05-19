package com.springboot.multi_tenant_rate_limiter.rateLimiter.tokenBucketImplementation.localRateLimiting;

import io.micrometer.core.annotation.Counted;
import io.micrometer.core.annotation.Timed;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.observation.annotation.Observed;
import reactor.core.publisher.Mono;

import java.util.concurrent.atomic.AtomicReference;

public class TokenBucket {

    private static final int MAX_CAS_RETRIES = 16;

    private final AtomicReference<BucketState> bucketState;
    private final MeterRegistry meterRegistry;

    public TokenBucket(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
        this.bucketState = new AtomicReference<>(
                new BucketState(0, System.nanoTime())
        );
    }

    public long getLastUpdatedTimestamp() {
        return bucketState.get().lastUpdatedTimestamp();
    }

    @Timed(
            value = "rate_limiter.local.allow.time",
            description = "Time spent evaluating local token bucket requests"
    )
    @Counted(
            value = "rate_limiter.local.allow.requests",
            description = "Local token bucket requests"
    )
    @Observed(name = "rate_limiter.local.allow")
    public Mono<Boolean> allowRequest() {
        return Mono.fromSupplier(() -> tryConsume(0));
    }

    @Timed(
            value = "rate_limiter.local.lease.time",
            description = "Time spent applying leased Redis tokens"
    )
    @Counted(
            value = "rate_limiter.local.lease.requests",
            description = "Local lease applications"
    )
    @Observed(name = "rate_limiter.local.lease")
    public Mono<Boolean> addLeaseAndAllowRequest(long leasedTokens) {

        if (leasedTokens <= 0) {
            return Mono.just(false);
        }

        return Mono.fromSupplier(() -> tryConsume(leasedTokens));
    }

    private boolean tryConsume(long additionalTokens) {

        for (int retry = 0; retry < MAX_CAS_RETRIES; retry++) {

            BucketState currentState = bucketState.get();

            long availableTokens =
                    currentState.tokens() + additionalTokens;

            boolean allowed = availableTokens >= 1;

            long remainingTokens = allowed
                    ? availableTokens - 1
                    : availableTokens;

            BucketState updatedState = new BucketState(
                    remainingTokens,
                    System.nanoTime()
            );

            if (bucketState.compareAndSet(currentState, updatedState)) {

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
                "rate_limiter.local.allow.outcome",
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
            long tokens,
            long lastUpdatedTimestamp
    ) {
    }
}