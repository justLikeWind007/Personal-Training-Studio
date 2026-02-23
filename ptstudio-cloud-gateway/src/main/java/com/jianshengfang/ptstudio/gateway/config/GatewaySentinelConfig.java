package com.jianshengfang.ptstudio.gateway.config;

import com.alibaba.csp.sentinel.adapter.gateway.sc.callback.BlockRequestHandler;
import com.alibaba.csp.sentinel.adapter.gateway.sc.callback.GatewayCallbackManager;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.core.publisher.Mono;

import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Configuration
public class GatewaySentinelConfig {

    @PostConstruct
    public void initBlockHandler() {
        BlockRequestHandler handler = (exchange, throwable) -> {
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("code", "GATEWAY_FLOW_LIMITED");
            body.put("message", "请求过于频繁，请稍后重试");
            body.put("path", exchange.getRequest().getPath().value());
            body.put("timestamp", OffsetDateTime.now().toString());
            body.put("status", 429);
            return ServerResponse.status(HttpStatus.TOO_MANY_REQUESTS)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(BodyInserters.fromValue(body));
        };
        GatewayCallbackManager.setBlockHandler(handler);
    }
}
