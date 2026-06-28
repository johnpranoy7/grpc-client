package com.johnp.grpcclient.config;

import com.johnp.grpcclient.websocket.AdvisingWebSocketHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

/**
 * Registers the bidirectional gRPC demo as a WebSocket endpoint.
 * <p>
 * Bidi streams cannot be mapped with {@code @GetMapping}/{@code @PostMapping} in a
 * {@code @RestController}. Spring WebSocket uses a separate registration API instead.
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    private final AdvisingWebSocketHandler advisingWebSocketHandler;

    @Value("${app.cors.allowed-origins:http://localhost:4200}")
    private String allowedOrigins;

    public WebSocketConfig(AdvisingWebSocketHandler advisingWebSocketHandler) {
        this.advisingWebSocketHandler = advisingWebSocketHandler;
    }

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(advisingWebSocketHandler, "/ws/demo/advising")
                .setAllowedOrigins(allowedOrigins.split(","));
    }
}
