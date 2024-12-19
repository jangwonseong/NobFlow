package com.samsa.service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import org.springframework.stereotype.Service;
import com.samsa.websocket.LogWebSocketHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;

/**
 * 로그 파일을 실시간으로 모니터링하고 WebSocket을 통해 전송하는 서비스 클래스
 * 파일 시스템의 로그를 실시간으로 읽어 연결된 클라이언트들에게 브로드캐스트
 */
@Service
@Slf4j
public class SimpleLogMonitor {
    
    /**
     * 모니터링할 로그 파일의 경로
     * application.properties/yml에서 logging.file.path 설정값을 주입받음
     */
    @Value("${logging.file.path}/logs.log")
    private String logFilePath;
    
    /**
     * 모니터링 실행 상태를 제어하는 플래그
     */
    private volatile boolean running = true;

    /**
     * 로그 파일 모니터링을 시작하는 메서드
     * 파일이 존재하지 않으면 생성하고, 파일의 마지막부터 읽기 시작
     * 새로운 로그 라인이 추가될 때마다 WebSocket으로 브로드캐스트
     */
    public void startMonitoring() {
        try {
            File logFile = new File(logFilePath);
            if (!logFile.exists() && (!logFile.getParentFile().mkdirs() || !logFile.createNewFile())) {
                    log.error("로그 파일 생성 실패");
                    return;
                }
            
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(new FileInputStream(logFile), StandardCharsets.UTF_8))) {
                
                long fileLength = logFile.length();
                if (fileLength > 0) {
                    reader.skip(fileLength);
                }

                while (running) {
                    String line = reader.readLine();
                    if (line != null) {
                        LogWebSocketHandler.broadcastLog(line);
                    } else {
                        Thread.sleep(100);
                    }
                }
                
            }
        } catch (Exception e) {
            log.error("로그 모니터링 중 오류 발생: {}", e.getMessage());
        }
    }
    /**
     * 로그 모니터링을 중지하는 메서드
     */
    public void stop() {
        running = false;
    }
}

/*
클래스 설명
목적: 로그 파일의 실시간 모니터링 및 WebSocket 브로드캐스팅
주요 기능:
지정된 로그 파일의 실시간 모니터링
새로운 로그 발생 시 WebSocket을 통한 즉시 전송
파일 시스템 감시 및 로그 스트리밍
핵심 구성요소:
파일 시스템 모니터링 로직
UTF-8 인코딩을 통한 로그 파일 읽기
비동기 로그 전송 메커니즘
안전한 시작/중지 기능
 */