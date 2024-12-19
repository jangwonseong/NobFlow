package com.samsa.service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import org.springframework.stereotype.Service;
import com.samsa.websocket.LogWebSocketHandler;

import lombok.extern.slf4j.Slf4j;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;

@Service
@Slf4j
public class SimpleLogMonitor {
    @Value("${logging.file.path}/logs.log")
    private String logFilePath;
    
    private volatile boolean running = true;

    public void startMonitoring() {
        try {
            File logFile = new File(logFilePath);
            if (!logFile.exists()) {
                logFile.getParentFile().mkdirs();
                logFile.createNewFile();
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

    public void stop() {
        running = false;
    }
}
