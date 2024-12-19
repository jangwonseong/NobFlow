package com.samsa.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.samsa.LogParser;

import lombok.extern.slf4j.Slf4j;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.time.format.DateTimeFormatter;

@Slf4j
@RestController
@RequestMapping("/api/logs")
public class LogAnalysisController {
    private final LogParser logParser;
    
    @Autowired
    public LogAnalysisController(LogParser logParser) {
        this.logParser = logParser;
    }

    @GetMapping("/filter")
    public ResponseEntity<Map<String, Object>> filterLogs(
            @RequestParam(required = false) String level,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime) {
        
        try {
            List<String> logs;
            if (level != null) {
                logs = logParser.filterLogsByLevel(level);
            } else if (startTime != null && endTime != null) {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
                LocalDateTime start = LocalDateTime.parse(startTime, formatter);
                LocalDateTime end = LocalDateTime.parse(endTime, formatter);
                logs = logParser.getLogsByTimeRange(start, end);
            } else {
                return ResponseEntity.badRequest().body(Map.of(
                    "error", "필터링 조건이 지정되지 않았습니다."
                ));
            }
            
            return ResponseEntity.ok(Map.of(
                "logs", logs,
                "count", logs.size()
            ));
            
        } catch (Exception e) {
            log.error("로그 필터링 중 오류 발생: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of(
                "error", "로그 필터링 중 오류가 발생했습니다: " + e.getMessage()
            ));
        }
    }

    @GetMapping("/summary")
    public ResponseEntity<Map<String, Object>> getLogSummary() {
        try {
            Map<String, Object> summary = new HashMap<>();
            summary.put("error", logParser.filterLogsByLevel("ERROR").size());
            summary.put("warn", logParser.filterLogsByLevel("WARN").size());
            summary.put("info", logParser.filterLogsByLevel("INFO").size());
            summary.put("debug", logParser.filterLogsByLevel("DEBUG").size());
            
            return ResponseEntity.ok(summary);
        } catch (Exception e) {
            log.error("로그 요약 생성 중 오류 발생: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of(
                "error", "로그 요약 생성 중 오류가 발생했습니다: " + e.getMessage()
            ));
        }
    }
}
