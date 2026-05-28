import http from 'k6/http';
import { check, sleep } from 'k6';

/**
 * K6 Load Test Scenarios for Multi-Tenant Rate Limiter
 * 
 * Tests different tenant tiers under various load patterns:
 * - FREE: 100 req/min (1.67 req/sec)
 * - PRO: 1000 req/min (16.67 req/sec)  
 * - ENTERPRISE: Unlimited
 */

export const options = {
  stages: [
    // Ramp-up: linearly increase users
    { duration: '2m', target: 10 },
    // Stay at peak for steady state testing
    { duration: '3m', target: 10 },
    // Ramp-down: linearly decrease users
    { duration: '1m', target: 0 },
  ],
  thresholds: {
    http_req_duration: ['p(99)<500'],  // P99 latency < 500ms
    'http_req_duration{tier:free}': ['p(99)<200'],  // FREE tier P99 < 200ms
    'http_req_duration{tier:pro}': ['p(99)<300'],   // PRO tier P99 < 300ms
    'http_req_duration{tier:enterprise}': ['p(99)<500'],  // ENTERPRISE tier P99 < 500ms
    http_req_failed: ['rate<0.1'],  // Error rate < 10%
  },
};

/**
 * FREE Tier Test: Should hit rate limits
 */
export function testFreeTier() {
  const payload = JSON.stringify({
    tier: 'FREE',
    data: `Request ${Date.now()}`,
  });

  const params = {
    headers: {
      'Content-Type': 'application/json',
      'X-Tenant-Tier': 'FREE',
    },
    tags: { tier: 'free' },
  };

  const res = http.post('http://localhost:8080/api/payments/v1/payments', payload, params);

  check(res, {
    'FREE tier: status is 200 or 429': (r) => r.status === 200 || r.status === 429,
    'FREE tier: response time < 200ms': (r) => r.timings.duration < 200,
  });

  sleep(1);
}

/**
 * PRO Tier Test: Higher throughput with occasional rate limits
 */
export function testProTier() {
  const payload = JSON.stringify({
    tier: 'PRO',
    data: `Request ${Date.now()}`,
  });

  const params = {
    headers: {
      'Content-Type': 'application/json',
      'X-Tenant-Tier': 'PRO',
    },
    tags: { tier: 'pro' },
  };

  const res = http.post('http://localhost:8080/api/orders/v1/orders', payload, params);

  check(res, {
    'PRO tier: status is 200 or 429': (r) => r.status === 200 || r.status === 429,
    'PRO tier: response time < 300ms': (r) => r.timings.duration < 300,
  });

  sleep(0.5);
}

/**
 * ENTERPRISE Tier Test: No rate limiting, high throughput expected
 */
export function testEnterpriseTier() {
  const payload = JSON.stringify({
    tier: 'ENTERPRISE',
    data: `Request ${Date.now()}`,
  });

  const params = {
    headers: {
      'Content-Type': 'application/json',
      'X-Tenant-Tier': 'ENTERPRISE',
    },
    tags: { tier: 'enterprise' },
  };

  const res = http.post('http://localhost:8080/api/payments/v1/payments', payload, params);

  check(res, {
    'ENTERPRISE tier: status is 200': (r) => r.status === 200,
    'ENTERPRISE tier: response time < 500ms': (r) => r.timings.duration < 500,
  });

  sleep(0.1);
}

/**
 * Health Check Test: Verify system is operational
 */
export function healthCheck() {
  const res = http.get('http://localhost:8080/actuator/health');

  check(res, {
    'health check: status is 200': (r) => r.status === 200,
    'health check: body is valid': (r) => r.body.includes('UP'),
  });
}

/**
 * Mixed Workload: Realistic scenario with different tier distributions
 * - 50% FREE tier (baseline users)
 * - 40% PRO tier (premium users)
 * - 10% ENTERPRISE tier (large customers)
 */
export function mixedWorkload() {
  const random = Math.random();

  if (random < 0.5) {
    testFreeTier();
  } else if (random < 0.9) {
    testProTier();
  } else {
    testEnterpriseTier();
  }
}

/**
 * Stress Test: Push system to limits
 * Run with: k6 run --stage 0s:0 --stage 30s:100 --stage 60s:0 k6-load-test.js
 */
export function stressTest() {
  testFreeTier();
  testProTier();
  testEnterpriseTier();
}