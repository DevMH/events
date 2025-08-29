package com.devmh.messaging_starter.starter;

import com.devmh.messaging_starter.core.FanoutAuthorization;
import com.devmh.messaging_starter.core.SessionRegistry;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.messaging.simp.SimpMessagingTemplate;

@AutoConfiguration
@EnableConfigurationProperties(EventBusProperties.class)
public class EventBusAutoConfiguration {
    @Bean @ConditionalOnMissingBean
    FanoutAuthorization fanoutAuthorization() {
        return (principal, event) -> true;
    }

    @Bean FanoutListener fanoutListener(EventBusProperties p, SimpMessagingTemplate ws,
                                        SessionRegistry reg, FanoutAuthorization authz) {
        return new FanoutListener(p, ws, reg, authz);
    }
}
