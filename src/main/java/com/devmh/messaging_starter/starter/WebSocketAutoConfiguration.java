package com.devmh.messaging_starter.starter;

import com.devmh.messaging_starter.core.PrincipalResolver;
import com.devmh.messaging_starter.core.SessionRegistry;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.messaging.simp.config.ChannelRegistration;
import org.springframework.messaging.simp.config.MessageBrokerRegistry;
import org.springframework.web.socket.config.annotation.*;

@AutoConfiguration
@EnableConfigurationProperties(EventBusProperties.class)
public class WebSocketAutoConfiguration implements WebSocketMessageBrokerConfigurer {
    private final EventBusProperties props;
    private final SessionEventsListener sessionEvents;

    public WebSocketAutoConfiguration(EventBusProperties p, SessionEventsListener s) {
        this.props = p; this.sessionEvents = s;
    }

    @Bean @ConditionalOnMissingBean
    SessionRegistry sessionRegistry() {
        return new SessionRegistry();
    }

    @Bean @ConditionalOnMissingBean
    PrincipalResolver principalResolver() {
        return (orig, attrs) -> orig;
    }

    @Bean SessionEventsListener sessionEvents(SessionRegistry r, PrincipalResolver pr) {
        return new SessionEventsListener(r, pr);
    }

    @Override public void registerStompEndpoints(StompEndpointRegistry registry) {
        registry.addEndpoint(props.getWsEndpoint()).setAllowedOriginPatterns("*");
        if (props.isSockJs()) registry.addEndpoint(props.getWsEndpoint()).setAllowedOriginPatterns("*").withSockJS();
    }
    @Override public void configureMessageBroker(MessageBrokerRegistry reg) {
        reg.enableSimpleBroker(props.getTopicPrefix(), props.getUserPrefix());
        reg.setApplicationDestinationPrefixes(props.getAppPrefix());
        reg.setUserDestinationPrefix(props.getUserPrefix());
    }
    @Override public void configureClientInboundChannel(ChannelRegistration reg) {
        reg.interceptors(sessionEvents);
    }
}
