package com.samsa.service;

import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
public class LogMonitorRunner implements CommandLineRunner {
    private final SimpleLogMonitor logMonitor;

    public LogMonitorRunner(SimpleLogMonitor logMonitor) {
        this.logMonitor = logMonitor;
    }

    @Override
    public void run(String... args) {
        new Thread(logMonitor::startMonitoring).start();
    }
}
