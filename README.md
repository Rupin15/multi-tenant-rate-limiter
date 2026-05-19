# Multi-Tenant Rate Limiter

## Overview

This repository implements a multi-module Spring Boot project demonstrating a distributed rate limiting gateway architecture. The gateway uses local token buckets plus Redis-backed leases for throttling client requests before routing traffic to downstream services.

The repository is already Docker-ready: `order-service`, `payment-service`, `api-gateway`, and Redis are intended to be managed through Docker Compose.

## Services

- `api-gateway`
  - Spring Cloud Gateway service based on WebFlux for non-blocking concurrent request handling
  - Custom global rate limiting filter for client IPs
  - Uses reactive Redis and Lua scripting for distributed token lease handling
  - Exposes Actuator metrics and Prometheus scraping endpoints

- `payment-service`
  - Simple Spring Boot service
  - Healthcheck endpoint: `GET /v1/payments/healthcheck`

- `order-service`
  - Simple Spring Boot service
  - Healthcheck endpoint: `GET /v1/orders/healthcheck`

- `redis`
  - Used as the shared backend for distributed rate limiting
  - Configured in `docker-compose.yml`

- `prometheus`
  - Metrics scraping service

- `grafana`
  - Dashboard visualization service

## Architecture

- Requests flow through the API gateway.
- The gateway extracts the client IP and applies a local token bucket check.
- If the local bucket is exhausted, the gateway requests a lease from Redis using Lua scripts.
- Redis health is monitored and the gateway rejects requests when Redis becomes unavailable.
- Gateway routes are configured for the payment and order services.

## Docker Setup

The repository includes a root `docker-compose.yml` that defines:

- `redis`
- `payment-service`
- `order-service`
- `prometheus`
- `grafana`


## Run

From the repository root:

```bash
docker-compose up
```

If the API gateway is enabled in `docker-compose.yml`, it will proxy requests to:

- `/api/payments/**` → `payment-service`
- `/api/orders/**` → `order-service`

## Configuration

- Main gateway config: `api-gateway/src/main/resources/application.yml`
- Redis Lua scripts: `api-gateway/src/main/resources/script/tokenBucket.lua` and `api-gateway/src/main/resources/script/refillLocal.lua`
- Monitoring config: `monitoring/prometheus.yml`
- Grafana dashboard: `monitoring/grafana/dashboards/rate-limiter-dashboard.json`

## Notes

- Built with Java 25 and Spring Boot 4.0.6
- The root `pom.xml` defines the multi-module build
- Docker orchestration is the primary runtime mechanism for this project
