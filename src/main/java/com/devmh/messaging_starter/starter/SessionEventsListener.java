package com.devmh.messaging_starter.starter;

import com.devmh.messaging_starter.core.PrincipalResolver;
import com.devmh.messaging_starter.core.SessionRegistry;
import org.springframework.context.ApplicationListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.*;

import java.security.Principal;
import java.util.Map;

@Component
class SessionEventsListener implements ApplicationListener<AbstractSubProtocolEvent>, org.springframework.messaging.support.ChannelInterceptor {
    private final SessionRegistry registry;
    private final PrincipalResolver resolver;

    SessionEventsListener(SessionRegistry r, PrincipalResolver pr) {
        this.registry = r; this.resolver = pr;
    }

    @Override public void onApplicationEvent(AbstractSubProtocolEvent e) {
        var acc = StompHeaderAccessor.wrap(e.getMessage());
        String sid = acc.getSessionId();
        if (e instanceof SessionConnectEvent sce) {
            Map<String,Object> attrs = (Map<String,Object>) e.getMessage().getHeaders().get("simpSessionAttributes");
            Principal resolved = resolver.resolve(acc.getUser(), attrs == null? Map.of(): attrs);
            if (resolved != null) registry.put(resolved.getName(), sid, attrs == null? Map.of(): attrs);
        } else if (e instanceof SessionDisconnectEvent) {
            registry.remove(sid);
        }
    }
}
