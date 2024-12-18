package com.samsa.websocket.handler;

import java.io.IOException;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import org.springframework.web.socket.TextMessage;

@Component
public class LogWebSocketHandler extends TextWebSocketHandler {
    private static Set<WebSocketSession> sessions = ConcurrentHashMap.newKeySet();
    
    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessions.add(session);
    }
    
    public static void broadcastLog(String level, String message) {
        String logMessage = String.format("{\"level\":\"%s\",\"message\":\"%s\"}", 
            level, message);
        sessions.forEach(session -> {
            try {
                session.sendMessage(new TextMessage(logMessage));
            } catch (IOException e) {
                e.printStackTrace();
            }
        });
    }
}
