package com.devmh.messaging.interceptors;

import org.springframework.context.ApplicationListener;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.AbstractSubProtocolEvent;
import org.springframework.web.socket.messaging.SessionConnectEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import java.security.Principal;
import java.util.HashMap;
import java.util.Map;

@Component
public class StompSessionListener
        implements ApplicationListener<AbstractSubProtocolEvent> {

    private final WsSessionRegistry registry;

    public StompSessionListener(WsSessionRegistry registry) { this.registry = registry; }

    @Override
    public void onApplicationEvent(AbstractSubProtocolEvent ev) {
        var accessor = StompHeaderAccessor.wrap(ev.getMessage());
        String sid = accessor.getSessionId();
        Principal user = accessor.getUser();

        if (ev instanceof SessionConnectEvent) {
            // attrs from handshake (if any)
            Map<String, Object> attrs = new HashMap<>();
            var simpSessAttrs = (Map<String, Object>) ev.getMessage().getHeaders()
                    .get(SimpMessageHeaderAccessor.SESSION_ATTRIBUTES);
            if (simpSessAttrs != null) {
                if (simpSessAttrs.get("pki_dn") != null) attrs.put("pki_dn", simpSessAttrs.get("pki_dn"));
                if (simpSessAttrs.get("pki_pem") != null) attrs.put("pki_pem", simpSessAttrs.get("pki_pem"));
            }
            String name = (user != null ? user.getName() : "anonymous");
            registry.put(name, sid, attrs);
        } else if (ev instanceof SessionDisconnectEvent) {
            registry.remove(sid);
        }
    }
}
