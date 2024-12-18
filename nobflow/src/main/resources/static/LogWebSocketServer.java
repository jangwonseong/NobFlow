package com.samsa;

import java.io.IOException;
import java.util.concurrent.CopyOnWriteArraySet;
import javax.websocket.*;
import javax.websocket.server.ServerEndpoint;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

@ServerEndpoint("/logs")
@Slf4j
public class LogWebSocketServer {
    private static final CopyOnWriteArraySet<Session> sessions = new CopyOnWriteArraySet<>();
    private static final ObjectMapper mapper = new ObjectMapper();


    @OnOpen
    public void onOpen(Session session) {
        sessions.add(session);
        log.info("새로운 웹소켓 연결: {}", session.getId());
    }

    @OnClose
    public void onClose(Session session) {
        sessions.remove(session);
        log.info("웹소켓 연결 종료: {}", session.getId());
    }

    @OnError
    public void onError(Session session, Throwable error) {
        log.error("웹소켓 에러: {}", error.getMessage());
    }

    public static void broadcastLog(String level, String message) {
        try {
            String json = mapper.writeValueAsString(Map.of(
                "level", level,
                "message", message
            ));
            
            for (Session session : sessions) {
                session.getBasicRemote().sendText(json);
            }
        } catch (IOException e) {
            log.error("로그 브로드캐스트 실패: {}", e.getMessage());
        }
    }
}