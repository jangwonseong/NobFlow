package com.samsa.service;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

/**
 * 애플리케이션 시작 시 로그 모니터링을 실행하는 Runner 클래스
 */
@Component
public class LogMonitorRunner implements CommandLineRunner {
    
    /** 로그 모니터링 서비스 */
    private final SimpleLogMonitor logMonitor;

    /**
     * LogMonitorRunner 생성자
     * @param logMonitor 로그 모니터링 서비스 인스턴스
     */
    public LogMonitorRunner(SimpleLogMonitor logMonitor) {
        this.logMonitor = logMonitor;
    }

    /**
     * 애플리케이션 시작 시 로그 모니터링을 별도 스레드로 실행
     * @param args 실행 인자
     */
    @Override
    public void run(String... args) {
        new Thread(logMonitor::startMonitoring).start();
    }
}