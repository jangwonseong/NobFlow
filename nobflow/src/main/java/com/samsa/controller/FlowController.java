package com.samsa.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.samsa.core.Flow;
import com.samsa.core.FlowPool;

import lombok.extern.slf4j.Slf4j;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.samsa.FlowLoaderReflection;
@Slf4j
@RestController
@RequestMapping("/api/flow")
@CrossOrigin(origins = "*")
public class FlowController {
    private Thread flowThread;
    private FlowPool flowPool;
    private volatile boolean isRunning = false;

    @PostMapping("/run")
    public ResponseEntity<String> runFlow(@RequestBody String flowJson) {
        try {
            if (isRunning) {
                return ResponseEntity.badRequest().body("Flow is already running");
            }

            ObjectMapper mapper = new ObjectMapper();
            JsonNode jsonNode = mapper.readTree(flowJson);
            
            // Create new FlowPool instance
            flowPool = new FlowPool();
            Flow flow = FlowLoaderReflection.createFlow(jsonNode);
            flowPool.addFlow(flow);
            
            // Start flow in a new thread
            flowThread = new Thread(() -> {
                try {
                    isRunning = true;
                    flowPool.run();
                } catch (Exception e) {
                    log.error("Flow execution error", e);
                } finally {
                    isRunning = false;
                }
            });
            flowThread.start();
            
            return ResponseEntity.ok("Flow started");
        } catch (Exception e) {
            log.error("Flow execution failed", e);
            return ResponseEntity.internalServerError().body(e.getMessage());
        }
    }

    @PostMapping("/stop")
    public ResponseEntity<String> stopFlow() {
        try {
            if (!isRunning || flowPool == null) {
                return ResponseEntity.badRequest().body("No running flow");
            }

            // Stop the flow pool
            flowPool.stop();
            
            // Interrupt the flow thread
            if (flowThread != null && flowThread.isAlive()) {
                flowThread.interrupt();
            }
            
            isRunning = false;
            return ResponseEntity.ok("Flow stopped successfully");
        } catch (Exception e) {
            log.error("Failed to stop flow", e);
            return ResponseEntity.internalServerError().body("Failed to stop flow: " + e.getMessage());
        }
    }
}
