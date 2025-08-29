package com.devmh.messaging_starter.core;

public record SessionInfo(String sessionId, String userName, java.util.Map<String,Object> attrs) {

}