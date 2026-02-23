package com.jianshengfang.ptstudio.gateway.filter;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class InternalTokenRelayFilter implements GlobalFilter, Ordered {

    private static final String INTERNAL_TOKEN_HEADER = "X-Internal-Token";

    @Value("${security.internal-token.value:}")
    private String internalTokenValue;

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        if (internalTokenValue == null || internalTokenValue.isBlank()) {
            return chain.filter(exchange);
        }
        if (exchange.getRequest().getHeaders().containsKey(INTERNAL_TOKEN_HEADER)) {
            return chain.filter(exchange);
        }
        ServerHttpRequest request = exchange.getRequest()
                .mutate()
                .header(INTERNAL_TOKEN_HEADER, internalTokenValue)
                .build();
        return chain.filter(exchange.mutate().request(request).build());
    }

    @Override
    public int getOrder() {
        return -200;
    }
}
