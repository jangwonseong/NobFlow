package com.samsa.service;

import java.io.*;
import java.nio.charset.StandardCharsets;
import org.springframework.stereotype.Service;
import com.samsa.websocket.LogWebSocketHandler;

@Service
public class SimpleLogMonitor {
    private static final String LOG_FILE_PATH = "/home/nhnacademy/Desktop/NobFlow/nobflow/logs/logs.log";
    private volatile boolean running = true;

    public void startMonitoring() {
        try {
            File logFile = new File(LOG_FILE_PATH);
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
            System.err.println("로그 모니터링 실패: " + e.getMessage());
        }
    }

    public void stop() {
        running = false;
    }
}
