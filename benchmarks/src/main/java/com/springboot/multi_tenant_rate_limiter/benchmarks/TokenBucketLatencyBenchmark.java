package com.springboot.multi_tenant_rate_limiter.benchmarks;

import org.openjdk.jmh.annotations.*;
import java.util.concurrent.TimeUnit;

/**
 * Latency-focused benchmarks for rate limiter operations.
 * Measures tail latencies (P50, P90, P99) for different scenarios.
 */
@Fork(value = 1, warmups = 1)
@Warmup(iterations = 2, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 2, timeUnit = TimeUnit.SECONDS)
@BenchmarkMode({Mode.AverageTime, Mode.SampleTime})
@OutputTimeUnit(TimeUnit.MICROSECONDS)
public class TokenBucketLatencyBenchmark {

    @State(Scope.Benchmark)
    public static class BenchmarkState {
        private TokenBucketRateLimiter rateLimiter;

        @Setup(Level.Trial)
        public void setup() {
            // PRO tier for moderate load testing
            rateLimiter = new TokenBucketRateLimiter(1000, 60);
        }
    }

    /**
     * Measure average latency of single token acquisition.
     * Expected P99: < 10 microseconds
     */
    @Benchmark
    public boolean tokenAcquisitionLatency(BenchmarkState state) {
        return state.rateLimiter.tryAcquire();
    }

    /**
     * Measure latency with token unavailability (rate limit hit).
     * Expected P99: < 5 microseconds
     */
    @Benchmark
    public boolean tokenAcquisitionWhenRateLimited(BenchmarkState state) {
        // Try to acquire more tokens than available
        return state.rateLimiter.tryAcquire(100000);
    }

    /**
     * Batch token acquisition latency.
     * Expected P99: < 15 microseconds
     */
    @Benchmark
    public boolean batchTokenAcquisition(BenchmarkState state) {
        return state.rateLimiter.tryAcquire(50);
    }
}