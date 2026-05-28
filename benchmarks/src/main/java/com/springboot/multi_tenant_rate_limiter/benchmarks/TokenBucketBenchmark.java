package com.springboot.multi_tenant_rate_limiter.benchmarks;

import org.openjdk.jmh.annotations.*;
import org.openjdk.jmh.results.format.ResultFormatType;
import org.openjdk.jmh.runner.Runner;
import org.openjdk.jmh.runner.RunnerException;
import org.openjdk.jmh.runner.options.Options;
import org.openjdk.jmh.runner.options.OptionsBuilder;

import java.util.concurrent.TimeUnit;

/**
 * JMH Benchmark for Token Bucket rate limiter operations.
 * 
 * Measures throughput and latency for different tenant tiers:
 * - FREE: 100 requests/minute
 * - PRO: 1000 requests/minute
 * - ENTERPRISE: Unlimited
 * 
 * Run with: mvn package && java -jar benchmarks/target/benchmarks.jar
 */
@Fork(value = 1, warmups = 1)
@Warmup(iterations = 3, time = 1, timeUnit = TimeUnit.SECONDS)
@Measurement(iterations = 5, time = 2, timeUnit = TimeUnit.SECONDS)
@BenchmarkMode(Mode.Throughput)
@OutputTimeUnit(TimeUnit.SECONDS)
public class TokenBucketBenchmark {

    @State(Scope.Benchmark)
    public static class BenchmarkState {
        private TokenBucketRateLimiter freeTierLimiter;
        private TokenBucketRateLimiter proTierLimiter;
        private TokenBucketRateLimiter enterpriseTierLimiter;

        @Setup(Level.Trial)
        public void setup() {
            // FREE tier: 100 requests/minute = 1.67 per second
            freeTierLimiter = new TokenBucketRateLimiter(100, 60);
            
            // PRO tier: 1000 requests/minute = 16.67 per second
            proTierLimiter = new TokenBucketRateLimiter(1000, 60);
            
            // ENTERPRISE tier: 10000 requests/minute = 166.67 per second
            enterpriseTierLimiter = new TokenBucketRateLimiter(10000, 60);
        }
    }

    /**
     * Benchmark token bucket acquisition on FREE tier.
     * Expected: ~6000 ops/sec (1 token per ~167 microseconds)
     */
    @Benchmark
    public boolean acquireTokenFreeTier(BenchmarkState state) {
        return state.freeTierLimiter.tryAcquire();
    }

    /**
     * Benchmark token bucket acquisition on PRO tier.
     * Expected: ~16000 ops/sec
     */
    @Benchmark
    public boolean acquireTokenProTier(BenchmarkState state) {
        return state.proTierLimiter.tryAcquire();
    }

    /**
     * Benchmark token bucket acquisition on ENTERPRISE tier.
     * Expected: ~160000 ops/sec (minimal overhead)
     */
    @Benchmark
    public boolean acquireTokenEnterpriseTier(BenchmarkState state) {
        return state.enterpriseTierLimiter.tryAcquire();
    }

    /**
     * Benchmark multi-token acquisition (common for batch operations).
     */
    @Benchmark
    public boolean acquireMultipleTokensProTier(BenchmarkState state) {
        return state.proTierLimiter.tryAcquire(10);
    }

    public static void main(String[] args) throws RunnerException {
        Options opt = new OptionsBuilder()
                .include(TokenBucketBenchmark.class.getSimpleName())
                .resultFormat(ResultFormatType.JSON)
                .result("jmh-benchmark-results.json")
                .build();

        new Runner(opt).run();
    }
}