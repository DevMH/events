package com.devmh.messaging.interceptors;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.support.DefaultHandshakeHandler;

import java.security.Principal;
import java.util.Map;

@Component
public class WsHandshakeHandler extends DefaultHandshakeHandler {
    @Override
    protected Principal determineUser(ServerHttpRequest req,
                                      WebSocketHandler handler,
                                      Map<String, Object> attrs) {
        // If Spring Security already set a user, keep it
        Principal p = super.determineUser(req, handler, attrs);
        if (p != null) return p;

        String dn = (String) attrs.get("pki_dn");
        if (dn != null) {
            return () -> dn; // very simple Principal; replace with an Authentication if desired
        }
        return null;
    }
}

