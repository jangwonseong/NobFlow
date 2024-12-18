package com.samsa;

import java.util.HashMap;
import java.util.Map;

import com.samsa.core.Flow;
import com.samsa.core.FlowPool;
import com.samsa.core.Pipe;
import com.samsa.node.in.DebugNode;
import com.samsa.node.in.InfluxNode;
import com.samsa.node.inout.DelayNode;
import com.samsa.node.inout.FunctionNode;
import com.samsa.node.out.MqttInNode;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class MqttToInfluxFlow {
    public static void main(String[] args) {
        FlowPool flowPool = new FlowPool();

        InfluxNode influxNode = new InfluxNode("http://192.168.71.221:8086",
                "7BuWmxpqHzyY7dN6jZiysF8SvdXEdnccAq9Uq23Mr7uL9NGc2tkZKrIkiTmaD4QtXY7NxaJSG8hbqs_kvu1kZQ==",
                "seong", "nhnacademy");

        MqttInNode mqttInNode =
                new MqttInNode("tcp://192.168.70.203:1883", "odngodng", new String[] {"data/#"});
        DelayNode delayNode = new DelayNode(10);
        DebugNode debugNode = new DebugNode();

        FunctionNode functionNode = new FunctionNode(message -> {
            try {
                String rawPayload = message.getPayload().toString();
                String payload = rawPayload.substring(1, rawPayload.length() - 1);

                String topic = payload.split(",", 2)[0];
                String jsonPart = payload.split(",", 2)[1].trim();

                if (topic.contains("lora") || topic.contains("power_meter")) {
                    return;
                }

                // topic 파싱
                String[] topicParts = topic.split("/");
                String deviceName = topicParts[10];
                String measureType = topicParts[topicParts.length - 1];

                // JSON 파싱
                String timeStr = jsonPart.split("\\{")[1].split(",")[0].split(":")[1];
                String valueStr = jsonPart.split("value\":")[1].split("}")[0];
                double value = Double.parseDouble(valueStr);

                Map<String, Object> newPayload = new HashMap<>();
                Map<String, String> tags = new HashMap<>();
                Map<String, Object> fields = new HashMap<>();

                // sensor 데이터 구성
                tags.put("deviceName", deviceName);
                fields.put(measureType, value); // temperature 등을 필드명으로 사용

                newPayload.put("measurement", "sensor");
                newPayload.put("tags", tags);
                newPayload.put("fields", fields);
                newPayload.put("time", Long.parseLong(timeStr));

                message.setPayload(newPayload);

            } catch (Exception e) {
                log.error("Error processing sensor message: {}", e.getMessage());
            }
        });



        // Flow 설정
        setupFlow(flowPool, mqttInNode, delayNode, functionNode, influxNode, debugNode);
    }


    private static void setupFlow(FlowPool flowPool, MqttInNode mqttInNode, DelayNode delayNode,
            FunctionNode functionNode, InfluxNode influxNode, DebugNode debugNode) {
        Pipe mqttInToDelay = new Pipe();
        Pipe delayToFunction = new Pipe();
        Pipe functionToInflux = new Pipe();
        Pipe functionToDebug = new Pipe();

        mqttInNode.getPort().addPipe(mqttInToDelay);
        delayNode.getInPort().addPipe(mqttInToDelay);
        delayNode.getOutPort().addPipe(delayToFunction);
        functionNode.getInPort().addPipe(delayToFunction);
        functionNode.getInPort().addPipe(functionToDebug);
        debugNode.getPort().addPipe(functionToDebug);
        functionNode.getOutPort().addPipe(functionToInflux);
        influxNode.getPort().addPipe(functionToInflux);

        Flow mqttFlow = new Flow();
        mqttFlow.addNode(mqttInNode);
        mqttFlow.addNode(delayNode);
        mqttFlow.addNode(functionNode);
        mqttFlow.addNode(influxNode);
        mqttFlow.addNode(debugNode);

        flowPool.addFlow(mqttFlow);
        flowPool.run();
    }
}
