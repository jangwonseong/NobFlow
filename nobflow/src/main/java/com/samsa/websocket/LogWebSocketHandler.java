package com.samsa.websocket;

import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;
import java.util.concurrent.ConcurrentHashMap;

/**
 * WebSocket을 통한 로그 메시지 브로드캐스팅을 처리하는 핸들러 클래스
 * 연결된 모든 WebSocket 세션에 로그 메시지를 실시간으로 전송
 */
public class LogWebSocketHandler extends TextWebSocketHandler {
    
    /**
     * 연결된 WebSocket 세션들을 관리하는 동시성 해시맵
     * 키: 세션 ID, 값: WebSocket 세션 객체
     */
    private static final ConcurrentHashMap<String, WebSocketSession> sessions = new ConcurrentHashMap<>();

    /**
     * WebSocket 연결이 수립될 때 호출되는 메서드
     * 새로운 세션을 sessions 맵에 추가
     *
     * @param session 새로 연결된 WebSocket 세션
     */
    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        sessions.put(session.getId(), session);
    }

    /**
     * 로그 메시지를 모든 연결된 세션에 브로드캐스트하는 메서드
     * 연결이 끊어진 세션은 자동으로 제거
     *
     * @param logMessage 브로드캐스트할 로그 메시지
     */
    public static void broadcastLog(String logMessage) {
        TextMessage message = new TextMessage(logMessage);
        
        sessions.forEach((id, session) -> {
            try {
                if (session.isOpen()) {
                    session.sendMessage(message);
                } else {
                    sessions.remove(id);
                }
            } catch (Exception e) {
                sessions.remove(id);
            }
        });
    }
}

/*
클래스 설명
목적: WebSocket을 통한 실시간 로그 메시지 브로드캐스팅 처리
주요 기능:
WebSocket 세션 관리 및 연결 처리
로그 메시지의 실시간 브로드캐스팅
비정상 세션의 자동 정리
핵심 구성요소:
ConcurrentHashMap을 사용한 스레드 안전한 세션 관리
연결된 모든 클라이언트에 대한 메시지 브로드캐스팅
끊어진 연결 자동 감지 및 정리 기능
 */