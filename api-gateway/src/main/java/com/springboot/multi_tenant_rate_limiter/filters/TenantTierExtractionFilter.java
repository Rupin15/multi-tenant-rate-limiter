package com.springboot.multi_tenant_rate_limiter.filters;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.springboot.multi_tenant_rate_limiter.rateLimiter.tokenBucketImplementation.policy.repository.TenantTier;
import com.springboot.multi_tenant_rate_limiter.rateLimiter.tokenBucketImplementation.policy.services.RateLimitPolicyResolver;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferUtils;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpRequestDecorator;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class TenantTierExtractionFilter implements GlobalFilter, Ordered {

    private static final Set<String> METHODS_WITH_BODY = Set.of("POST", "PUT", "PATCH");
    private final ObjectMapper objectMapper;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        TenantTier explicitTier = extractTierFromHeadersOrQuery(exchange);
        if (explicitTier != null) {
            exchange.getAttributes().put(RateLimitPolicyResolver.TENANT_TIER_ATTRIBUTE, explicitTier);
            return chain.filter(exchange);
        }

        if (!isJsonBodyRequest(exchange)) {
            exchange.getAttributes().put(RateLimitPolicyResolver.TENANT_TIER_ATTRIBUTE, TenantTier.FREE);
            return chain.filter(exchange);
        }

        ServerHttpRequest request = exchange.getRequest();
        return DataBufferUtils.join(request.getBody())
                .flatMap(dataBuffer -> {
                    byte[] bytes = new byte[dataBuffer.readableByteCount()];
                    dataBuffer.read(bytes);
                    DataBufferUtils.release(dataBuffer);

                    TenantTier tier = extractTierFromBody(bytes);
                    exchange.getAttributes().put(RateLimitPolicyResolver.TENANT_TIER_ATTRIBUTE, tier);

                    ServerHttpRequest decorated = new ServerHttpRequestDecorator(request) {
                        @Override
                        public Flux<DataBuffer> getBody() {
                            DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(bytes);
                            return Flux.just(buffer);
                        }
                    };

                    return chain.filter(exchange.mutate().request(decorated).build());
                })
                .switchIfEmpty(chain.filter(exchange));
    }

    private TenantTier extractTierFromHeadersOrQuery(ServerWebExchange exchange) {
        String headerTier = exchange.getRequest().getHeaders().getFirst("X-Tenant-Tier");
        if (headerTier != null && !headerTier.isBlank()) {
            return TenantTier.from(headerTier);
        }

        String queryTier = exchange.getRequest().getQueryParams().getFirst("tier");
        if (queryTier != null && !queryTier.isBlank()) {
            return TenantTier.from(queryTier);
        }
        return null;
    }

    private boolean isJsonBodyRequest(ServerWebExchange exchange) {
        if (!METHODS_WITH_BODY.contains(exchange.getRequest().getMethod().name())) {
            return false;
        }
        MediaType contentType = exchange.getRequest().getHeaders().getContentType();
        return contentType != null && MediaType.APPLICATION_JSON.isCompatibleWith(contentType);
    }

    private TenantTier extractTierFromBody(byte[] requestBody) {
        if (requestBody.length == 0) {
            return TenantTier.FREE;
        }
        try {
            JsonNode root = objectMapper.readTree(new String(requestBody, StandardCharsets.UTF_8));
            JsonNode tierNode = root.path("tier");
            if (tierNode.isTextual()) {
                return TenantTier.from(tierNode.asText());
            }
        } catch (Exception exception) {
            log.debug("unable to parse request body for tier, defaulting to FREE reason={}", exception.getMessage());
        }
        return TenantTier.FREE;
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 1;
    }
}
