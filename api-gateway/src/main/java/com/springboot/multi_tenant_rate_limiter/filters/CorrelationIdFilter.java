package com.springboot.multi_tenant_rate_limiter.filters;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Component
public class CorrelationIdFilter implements GlobalFilter, Ordered {

    public static final String CORRELATION_ID_HEADER = "X-Correlation-Id";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        String correlationId = exchange.getRequest().getHeaders().getFirst(CORRELATION_ID_HEADER);
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }
        String resolvedCorrelationId = correlationId;

        ServerHttpRequest requestWithCorrelation = exchange.getRequest().mutate()
                .headers(headers -> headers.set(CORRELATION_ID_HEADER, resolvedCorrelationId))
                .build();
        exchange.getResponse().getHeaders().set(CORRELATION_ID_HEADER, resolvedCorrelationId);

        return chain.filter(exchange.mutate().request(requestWithCorrelation).build());
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
