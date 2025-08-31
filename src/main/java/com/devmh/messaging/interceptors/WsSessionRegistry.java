package com.devmh.messaging.interceptors;

import org.springframework.stereotype.Component;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
public class WsSessionRegistry {

    private final ConcurrentMap<String, SessionInfo> bySession = new ConcurrentHashMap<>();
    private final ConcurrentMap<String, Set<String>> sessionsByUser = new ConcurrentHashMap<>();

    public void put(String user, String sessionId, Map<String,Object> attrs) {
        bySession.put(sessionId, new SessionInfo(sessionId, user, attrs));
        sessionsByUser.computeIfAbsent(user, k -> ConcurrentHashMap.newKeySet()).add(sessionId);
    }
    public void remove(String sessionId) {
        SessionInfo info = bySession.remove(sessionId);
        if (info != null) {
            var set = sessionsByUser.getOrDefault(info.userName(), Set.of());
            set.remove(sessionId);
            if (set.isEmpty()) sessionsByUser.remove(info.userName());
        }
    }
    public Set<String> sessionsOf(String user) {
        return sessionsByUser.getOrDefault(user, Set.of());
    }
    public Optional<SessionInfo> get(String sessionId) {
        return Optional.ofNullable(bySession.get(sessionId));
    }
}
