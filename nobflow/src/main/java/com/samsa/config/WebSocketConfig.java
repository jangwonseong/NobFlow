package com.samsa.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

import com.samsa.websocket.LogWebSocketHandler;

/**
 * WebSocket 설정을 담당하는 구성 클래스
 * 
 * @Configuration 스프링 설정 클래스임을 나타냄
 * @EnableWebSocket WebSocket 기능을 활성화
 */
@Configuration
@EnableWebSocket
public class WebSocketConfig implements WebSocketConfigurer {

    /**
     * WebSocket 핸들러를 등록하는 메서드
     * 
     * @param registry WebSocket 핸들러 레지스트리
     * 로그 스트리밍을 위한 WebSocket 엔드포인트(/logs/stream)를 설정하고
     * CORS(Cross-Origin Resource Sharing)를 모든 출처에 대해 허용
     */
    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(new LogWebSocketHandler(), "/logs/stream")
               .setAllowedOrigins("*");
    }
}
