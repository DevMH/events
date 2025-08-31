package com.devmh.messaging.interceptors;

import java.util.Map;

public record SessionInfo(String sessionId, String userName, Map<String, Object> attrs) {
}