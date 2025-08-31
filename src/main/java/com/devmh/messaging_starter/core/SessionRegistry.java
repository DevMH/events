package com.devmh.messaging_starter.core;

import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class SessionRegistry {
    private final Map<String, SessionInfo> bySession = new ConcurrentHashMap<>();
    private final Map<String, java.util.Set<String>> sessionsByUser = new ConcurrentHashMap<>();

    public void put(String user, String sessionId, java.util.Map<String,Object> attrs) {
        bySession.put(sessionId, new SessionInfo(sessionId, user, attrs));
        sessionsByUser.computeIfAbsent(user, k -> ConcurrentHashMap.newKeySet()).add(sessionId);
    }
    public void remove(String sessionId) {
        SessionInfo info = bySession.remove(sessionId);
        if (info != null) {
            Set<String> set = sessionsByUser.getOrDefault(info.userName(), Set.of());
            set.remove(sessionId);
            if (set.isEmpty()) {
                sessionsByUser.remove(info.userName());
            }

        }
    }
    public Set<String> sessionsOf(String user) {
        return sessionsByUser.getOrDefault(user, java.util.Set.of());
    }

    public java.util.Optional<SessionInfo> get(String sessionId) {
        return java.util.Optional.ofNullable(bySession.get(sessionId));
    }
}
