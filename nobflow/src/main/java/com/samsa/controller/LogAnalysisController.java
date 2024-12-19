package com.samsa.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import com.samsa.LogParser;

import lombok.extern.slf4j.Slf4j;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/logs")
public class LogAnalysisController {
    private final LogParser logParser;
    
    public LogAnalysisController() {
        this.logParser = new LogParser("/home/nhnacademy/Desktop/NobFlow/nobflow/logs.log");
    }

    @GetMapping("/analyze")
    public Map<String, Object> analyzeLog() {
        Map<String, Object> result = new HashMap<>();
        
        try {
            // 기본 로그 분석
            result.put("logsByLevel", logParser.getLogsByLevel());
            result.put("errorLogs", logParser.getLogsBySpecificLevel(LogParser.LogLevel.ERROR));
            result.put("modbusLogs", logParser.searchLogsByKeyword("Modbus"));
            result.put("mqttLogs", logParser.searchLogsByKeyword("MQTT"));
            
            log.info("로그 분석이 성공적으로 완료되었습니다.");
        } catch (Exception e) {
            log.error("로그 분석 중 오류 발생: {}", e.getMessage());
            result.put("error", "로그 분석 실패: " + e.getMessage());
        }
        
        return result;
    }

    @GetMapping("/search")
    public ResponseEntity<?> searchLogs(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String level) {
        try {
            List<String> logs;
            if (level != null) {
                logs = logParser.getLogsBySpecificLevel(LogParser.LogLevel.valueOf(level));  // logParser 인스턴스 사용
            } else if (keyword != null) {
                logs = logParser.searchLogsByKeyword(keyword);
            } else {
                return ResponseEntity.badRequest().body("검색어나 로그 레벨을 지정해주세요.");
            }
            return ResponseEntity.ok(logs);
        } catch (Exception e) {
            log.error("로그 검색 중 오류 발생: {}", e.getMessage());
            return ResponseEntity.internalServerError().body("로그 검색 실패: " + e.getMessage());
        }
    }
    

    @GetMapping("/time-range")
    public ResponseEntity<?> getLogsByTimeRange(
            @RequestParam String startTime,
            @RequestParam String endTime) {
        try {
            LocalDateTime start = LocalDateTime.parse(startTime);
            LocalDateTime end = LocalDateTime.parse(endTime);
            List<String> logs = logParser.getLogsByTimeRange(start, end);
            return ResponseEntity.ok(logs);
        } catch (Exception e) {
            log.error("시간 범위 로그 검색 중 오류 발생: {}", e.getMessage());
            return ResponseEntity.internalServerError().body("시간 범위 로그 검색 실패: " + e.getMessage());
        }
    }

    @GetMapping("/summary")
    public ResponseEntity<?> getLogSummary() {
        try {
            Map<String, Object> summary = new HashMap<>();
            Map<String, List<String>> logsByLevel = logParser.getLogsByLevel();
            
            summary.put("totalLogs", logsByLevel.values().stream()
                .mapToInt(List::size).sum());
            summary.put("errorCount", logsByLevel.getOrDefault("ERROR", List.of()).size());
            summary.put("warnCount", logsByLevel.getOrDefault("WARN", List.of()).size());
            summary.put("infoCount", logsByLevel.getOrDefault("INFO", List.of()).size());
            
            return ResponseEntity.ok(summary);
        } catch (Exception e) {
            log.error("로그 요약 생성 중 오류 발생: {}", e.getMessage());
            return ResponseEntity.internalServerError().body("로그 요약 생성 실패: " + e.getMessage());
        }
    }
}
