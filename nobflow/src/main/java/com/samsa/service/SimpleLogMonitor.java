package com.samsa.service;

import java.io.File;
import java.io.RandomAccessFile;
import org.springframework.stereotype.Service;

@Service
public class SimpleLogMonitor {
    private static final String LOG_FILE_PATH = "/home/nhnacademy/Desktop/NobFlow/nobflow/logs.log";
    private volatile boolean running = true;

    public void startMonitoring() {
        try {
            File logFile = new File(LOG_FILE_PATH);
            if (!logFile.exists()) {
                logFile.getParentFile().mkdirs();
                logFile.createNewFile();
            }

            try (RandomAccessFile reader = new RandomAccessFile(LOG_FILE_PATH, "r")) {
                long lastPosition = reader.length();

                while (running) {
                    long length = reader.length();
                    if (length > lastPosition) {
                        reader.seek(lastPosition);
                        String line;
                        while ((line = reader.readLine()) != null) {
                            System.out.println(line);
                        }
                        lastPosition = reader.getFilePointer();
                    }
                    Thread.sleep(100);
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
