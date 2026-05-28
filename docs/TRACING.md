# Distributed Tracing & Correlation ID Guide

## Overview

Every request gets a unique `traceId` and `correlationId` that propagates through API Gateway → Config Server → Downstream Services. This enables debugging production issues and identifying performance bottlenecks.

## Correlation ID Flow

```
Client Request
  ↓
Generate X-Correlation-ID (if missing)
  ↓
API Gateway receives request
  Put correlationId in MDC
  ↓
Rate Limit Check Span
  - Local bucket check
  - Redis lease check
  - Policy lookup
  ↓
Forward to downstream service
  Pass X-Correlation-ID header
  ↓
Downstream service logs with correlationId
  ↓
Response back to gateway
  ↓
Export trace to Jaeger
```

## Log Output Example

With tracing enabled, logs show correlation IDs:

```
2026-05-28 20:15:32 [reactor-http-epoll-3] INFO  gateway.RateLimiter 
  [traceId=abc123def456 spanId=span-001 correlationId=req-12345] 
  Rate limit check: tier=PRO, allowed=true, duration=0.5ms

2026-05-28 20:15:32 [reactor-http-epoll-3] INFO  gateway.RouteFilter 
  [traceId=abc123def456 spanId=span-002 correlationId=req-12345] 
  Routing to payment-service

2026-05-28 20:15:33 [payment-worker-1] INFO  payment.Service 
  [traceId=abc123def456 spanId=span-003 correlationId=req-12345] 
  Processing payment: amount=100
```

## Rate Limit Decision Spans

Rate limit checks are captured as Jaeger spans:

```
Span: rate_limit_check
├─ Duration: 0.511ms
├─ Status: OK
├─ Attributes:
│  ├─ tier: "PRO"
│  ├─ endpoint: "/api/payments"
│  ├─ allowed: true
│  └─ correlationId: "req-12345"
├─ Events:
│  ├─ local_bucket_check (0.1µs)
│  ├─ redis_lease_check (0.4ms)
│  └─ policy_lookup (0.01ms)
└─ Links: [parent-trace-id]
```

## Viewing Traces

### Jaeger UI

Navigate to http://localhost:16686

**Search Examples**:

1. **Find all requests for a tier**:
   - Service: `api-gateway`
   - Tags: `tier=PRO`

2. **Find slow requests**:
   - Operation: `rate_limit_check`
   - Min Duration: `100ms`

3. **Find rate-limited requests**:
   - Tags: `allowed=false`

4. **Find requests by correlation ID**:
   - Tags: `correlationId=req-12345`

## Performance Impact

- **Memory**: ~1KB per span
- **CPU**: < 1% overhead
- **Network**: Async batched export (~1KB per 100 spans)

## Custom Span Creation

```java
@Component
public class CustomTracingExample {
    private final Tracer tracer;
    
    public void processPayment(String paymentId, BigDecimal amount) {
        Span span = tracer.spanBuilder("process_payment")
            .setAttribute("paymentId", paymentId)
            .setAttribute("amount", amount.toString())
            .startSpan();
        
        try (Scope scope = span.makeCurrent()) {
            // Your code here
            span.addEvent("payment_validated");
            // More code
            span.addEvent("payment_processed");
        } finally {
            span.end();
        }
    }
}
```

## Correlation ID Propagation

Headers propagated across services:

```
X-Trace-ID: abc123def456
X-Correlation-ID: req-12345
X-Span-ID: span-001
```

## Troubleshooting with Traces

### Slow Rate Limit Check

**Trace shows**: `redis_lease_check: 50ms (expected < 5ms)`

**Investigation**:
1. Check Redis latency: `redis-cli --latency`
2. Review circuit breaker state
3. Check connection pool utilization

### Correlation ID Not Propagating

**Logs from different services have different correlationIds**

**Fix**:
1. Verify `X-Correlation-ID` header passed downstream
2. Check MDC is restored in async contexts
3. Review TracingResponseFilter configuration
