# API Documentation

## Rate Limiter API Endpoints

### 1. Payment Gateway

```http
POST /api/payments/**
```

**Headers**:
- `X-Tenant-Tier: FREE|PRO|ENTERPRISE` (optional, defaults to FREE)
- `X-Correlation-ID: uuid` (optional, auto-generated)
- `Content-Type: application/json`

**Query Parameters**:
- `tier=FREE|PRO|ENTERPRISE` (alternative to header)

**Response Headers**:
- `X-RateLimit-Limit: 100` (requests per minute for tier)
- `X-RateLimit-Remaining: 45` (tokens left)
- `X-RateLimit-Reset: 1590771000` (Unix timestamp)
- `X-Trace-ID: abc123` (distributed trace ID)
- `X-Correlation-ID: req-12345` (request correlation ID)

**Success Response (200)**:
```json
{
  "data": { /* response from payment service */ },
  "traceId": "abc123def456"
}
```

**Rate Limited Response (429)**:
```json
{
  "error": "Rate limit exceeded",
  "tier": "FREE",
  "limit": 100,
  "remaining": 0,
  "retryAfter": 45
}
```

**Service Unavailable (503)**:
```json
{
  "error": "Rate limiter not ready",
  "reason": "Configuration sync in progress"
}
```

### 2. Order Gateway

```http
POST /api/orders/**
```

Same headers and responses as Payment Gateway.

### 3. Health Check

```http
GET /actuator/health
```

**Response (200)**:
```json
{
  "status": "UP",
  "components": {
    "redis": {
      "status": "UP",
      "details": {
        "version": "7.0.0"
      }
    },
    "circuitBreakers": {
      "status": "UP",
      "details": {
        "redisLease": "CLOSED"
      }
    }
  }
}
```

### 4. Metrics

```http
GET /actuator/prometheus
```

Returns Prometheus format metrics.

**Key Metrics**:
- `rate_limit_calls_total{tier, status}` - Total calls
- `rate_limit_rejection_total{tier}` - Rejected calls
- `rate_limit_duration_seconds{tier, operation, quantile}` - Latency
- `redis_lease_duration_ms{tier}` - Redis latency
- `circuit_breaker_state{name}` - Circuit breaker status

## Tier Specifications

| Tier | Requests/Min | Notes |
|------|-------------|-------|
| FREE | 100 | Baseline users |
| PRO | 1,000 | Premium users |
| ENTERPRISE | Unlimited | Large customers |

## Error Codes

| Code | Reason | Action |
|------|--------|--------|
| 200 | Request allowed | None |
| 429 | Rate limit exceeded | Retry after X seconds |
| 503 | Rate limiter not ready | Retry after 5 seconds |
| 504 | Gateway timeout | Retry with backoff |

## Examples

### cURL

```bash
# Request with FREE tier
curl -X POST http://localhost:8080/api/payments/v1/payments \
  -H "X-Tenant-Tier: FREE" \
  -H "Content-Type: application/json" \
  -d '{"amount": 100}'

# Request with PRO tier
curl -X POST http://localhost:8080/api/orders/v1/orders \
  -H "X-Tenant-Tier: PRO" \
  -d '{"items": [...]}'

# Health check
curl http://localhost:8080/actuator/health | jq .
```

### Python

```python
import requests

headers = {
    "X-Tenant-Tier": "PRO",
    "X-Correlation-ID": "req-12345"
}

response = requests.post(
    "http://localhost:8080/api/payments/v1/payments",
    headers=headers,
    json={"amount": 100}
)

if response.status_code == 429:
    retry_after = response.headers.get("X-RateLimit-Reset")
    print(f"Rate limited. Retry after {retry_after}")
else:
    print(response.json())
```

### Node.js

```javascript
const axios = require('axios');

const client = axios.create({
  baseURL: 'http://localhost:8080',
  headers: {
    'X-Tenant-Tier': 'PRO'
  }
});

try {
  const response = await client.post('/api/payments/v1/payments', {
    amount: 100
  });
  console.log(response.data);
} catch (error) {
  if (error.response.status === 429) {
    const retryAfter = error.response.headers['x-ratelimit-reset'];
    console.log(`Rate limited. Retry after ${retryAfter}`);
  }
}
```

## Rate Limit Strategy

**Local Bucket Check** (< 1µs):
- Very fast, eventual consistency
- 95% of requests resolved here

**Redis Lease Check** (5-10ms):
- Global rate limit enforcement
- Used when local bucket exhausted
- Circuit breaker protects against Redis failures

**Fallback** (Redis down):
- Reject requests (fail-safe)
- Prevents thundering herd
- Circuit breaker automatically recovers
