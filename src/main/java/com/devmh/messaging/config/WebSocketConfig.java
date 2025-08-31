package com.devmh.messaging.config;

import com.devmh.messaging.interceptors.WsHandshakeHandler;
import com.devmh.messaging.interceptors.WsHandshakeInterceptor;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Configuration;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.web.socket.config.annotation.EnableWebSocketMessageBroker;
import org.springframework.web.socket.config.annotation.StompEndpointRegistry;
import org.springframework.web.socket.config.annotation.WebSocketMessageBrokerConfigurer;

@Slf4j
@Configuration
@EnableWebSocketMessageBroker
public class WebSocketConfig implements WebSocketMessageBrokerConfigurer {

    @Override
    public void registerStompEndpoints(StompEndpointRegistry registry) {
        // Browser connects here (e.g., ws://host:port/ws) and then uses STOMP
        registry.addEndpoint("/ws")
                .addInterceptors(new WsHandshakeInterceptor())
                .setHandshakeHandler(new WsHandshakeHandler())
                .setAllowedOriginPatterns("*")
                .withSockJS();
        registry.addEndpoint("/ws")
                .addInterceptors(new WsHandshakeInterceptor())
                .setHandshakeHandler(new WsHandshakeHandler())
                .setAllowedOriginPatterns("*");
    }

    @Override
    public void configureMessageBroker(MessageBrokerRegistry registry) {
        registry.enableSimpleBroker("/topic");
        registry.setApplicationDestinationPrefixes("/app");
    }

    @Override
    public void configureClientOutboundChannel(ChannelRegistration reg) {
        // add auth check interceptor
        reg.interceptors(new ChannelInterceptor() {
            @Override
            public Message<?> preSend(@NonNull Message<?> m, @NonNull MessageChannel c) {
                log.info("WebSocket outbound: {} {} {}", m, m.getHeaders().get("simpDestination"), c);
                return m;
            }
        });
    }
}
