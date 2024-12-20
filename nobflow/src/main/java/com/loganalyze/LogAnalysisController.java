package com.loganalyze;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import lombok.extern.slf4j.Slf4j;
import java.time.LocalDateTime;
import java.util.ArrayList;
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
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime) {
        
        try {
            List<String> logs = new ArrayList<>();
            
            // 필터 조건에 따른 로그 필터링
            if (level != null) {
                logs = logParser.filterLogsByLevel(level.toUpperCase());
            } else if (keyword != null) {
                logs = logParser.searchLogsByKeyword(keyword);
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
            
            Map<String, Object> response = new HashMap<>();
            response.put("logs", logs);
            response.put("count", logs.size());
            response.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            
            return ResponseEntity.ok(response);
            
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
            Map<String, Integer> levelCounts = logParser.getLogSummary();
            
            summary.put("logLevels", levelCounts);
            summary.put("totalCount", levelCounts.values().stream().mapToInt(Integer::intValue).sum());
            summary.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            
            // 에러 비율 계산
            double errorRate = calculateErrorRate(levelCounts);
            summary.put("errorRate", String.format("%.2f%%", errorRate));
            
            return ResponseEntity.ok(summary);
        } catch (Exception e) {
            log.error("로그 요약 생성 중 오류 발생: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of(
                "error", "로그 요약 생성 중 오류가 발생했습니다: " + e.getMessage()
            ));
        }
    }

    @GetMapping("/analysis")
    public ResponseEntity<Map<String, Object>> analyzeLogPatterns() {
        try {
            Map<String, Object> analysis = new HashMap<>();
            
            // 노드 실행 패턴 분석
            List<String> nodeExecutions = logParser.searchLogsByKeyword("노드 실행");
            analysis.put("nodeExecutionCount", nodeExecutions.size());
            
            // MQTT 메시지 패턴 분석
            List<String> mqttMessages = logParser.searchLogsByKeyword("MQTT");
            analysis.put("mqttMessageCount", mqttMessages.size());
            
            // 에러 패턴 분석
            List<String> errors = logParser.filterLogsByLevel("ERROR");
            analysis.put("errorCount", errors.size());
            
            // 타임스탬프 추가
            analysis.put("timestamp", LocalDateTime.now().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME));
            
            return ResponseEntity.ok(analysis);
        } catch (Exception e) {
            log.error("로그 패턴 분석 중 오류 발생: {}", e.getMessage());
            return ResponseEntity.internalServerError().body(Map.of(
                "error", "로그 패턴 분석 중 오류가 발생했습니다: " + e.getMessage()
            ));
        }
    }

    private double calculateErrorRate(Map<String, Integer> levelCounts) {
        int totalLogs = levelCounts.values().stream().mapToInt(Integer::intValue).sum();
        int errorLogs = levelCounts.getOrDefault("ERROR", 0);
        return totalLogs > 0 ? (errorLogs * 100.0) / totalLogs : 0.0;
    }
}
