package com.samsa.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.samsa.core.Flow;
import com.samsa.core.FlowPool;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.samsa.FlowLoaderReflection;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

@Slf4j
@RestController
@RequestMapping("/api/flow")
@CrossOrigin(origins = "*")
public class FlowController {
    private static final int FLOW_SHUTDOWN_TIMEOUT = 30;
    private volatile CompletableFuture<Void> flowFuture;
    private FlowPool flowPool;
    private final Object flowLock = new Object();
    private volatile boolean isRunning = false;

    @PostMapping("/run")
    public ResponseEntity<?> runFlow(@RequestBody String flowJson) {
        synchronized (flowLock) {
            if (isRunning) {
                log.warn("Flow 실행 요청이 거부됨: 이미 실행 중");
                return ResponseEntity.badRequest().body("Flow가 이미 실행 중입니다");
            }

            try {
                ObjectMapper mapper = new ObjectMapper();
                JsonNode jsonNode = mapper.readTree(flowJson);
                
                flowPool = new FlowPool();
                Flow flow = FlowLoaderReflection.createFlow(jsonNode);
                flowPool.addFlow(flow);
                
                flowFuture = CompletableFuture.runAsync(() -> {
                    try {
                        isRunning = true;
                        log.info("Flow 실행 시작");
                        flowPool.run();
                    } catch (Exception e) {
                        log.error("Flow 실행 중 오류 발생", e);
                        throw e;
                    }
                }).exceptionally(throwable -> {
                    log.error("Flow 실행 실패", throwable);
                    isRunning = false;
                    return null;
                });
                
                return ResponseEntity.ok().body(Map.of(
                    "status", "success",
                    "message", "Flow가 성공적으로 시작되었습니다"
                ));
            } catch (Exception e) {
                log.error("Flow 시작 실패", e);
                return ResponseEntity.internalServerError().body(Map.of(
                    "status", "error",
                    "message", "Flow 시작 실패: " + e.getMessage()
                ));
            }
        }
    }

    @PostMapping("/stop")
    public ResponseEntity<?> stopFlow() {
        synchronized (flowLock) {
            if (!isRunning || flowPool == null) {
                log.warn("Flow 중지 요청이 거부됨: 실행 중인 Flow가 없음");
                return ResponseEntity.badRequest().body(Map.of(
                    "status", "error",
                    "message", "실행 중인 Flow가 없습니다"
                ));
            }

            try {
                log.info("Flow 중지 시작");
                flowPool.stop();
                
                if (flowFuture != null) {
                    boolean terminated = flowFuture.cancel(true);
                    if (!terminated) {
                        flowFuture.get(FLOW_SHUTDOWN_TIMEOUT, TimeUnit.SECONDS);
                    }
                }
                
                isRunning = false;
                log.info("Flow가 성공적으로 중지됨");
                
                return ResponseEntity.ok().body(Map.of(
                    "status", "success",
                    "message", "Flow가 성공적으로 중지되었습니다"
                ));
            } catch (Exception e) {
                log.error("Flow 중지 실패", e);
                return ResponseEntity.internalServerError().body(Map.of(
                    "status", "error",
                    "message", "Flow 중지 실패: " + e.getMessage()
                ));
            }
        }
    }

    @GetMapping("/status")
    public ResponseEntity<?> getFlowStatus() {
        return ResponseEntity.ok().body(Map.of(
            "isRunning", isRunning,
            "activeFlows", flowPool != null ? 1 : 0  // Simply return 1 if pool exists
        ));
    }
    
}
