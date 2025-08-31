package com.devmh.messaging.interceptors;

import lombok.NonNull;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

@Component
public class WsHandshakeInterceptor implements HandshakeInterceptor {
    @Override
    public boolean beforeHandshake(@NonNull ServerHttpRequest req, @NonNull ServerHttpResponse res,
                                   @NonNull WebSocketHandler wsHandler, @NonNull Map<String,Object> attrs) {
        if (req instanceof ServletServerHttpRequest sreq) {
            var http = sreq.getServletRequest();
            String dn = http.getHeader("x-ssl-client-dn");
            String pem = http.getHeader("ssl-client-cert"); // URL-escaped PEM, proxy-dependent
            if (dn != null) attrs.put("pki_dn", dn);
            if (pem != null) attrs.put("pki_pem", pem);
        }
        return true;
    }
    @Override public void afterHandshake(@NonNull ServerHttpRequest r, @NonNull ServerHttpResponse s,
                                         @NonNull WebSocketHandler h, Exception ex) {}
}
