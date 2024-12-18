package com.samsa;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.samsa.core.Flow;
import com.samsa.core.Pipe;
import com.samsa.core.node.Node;
import com.samsa.core.node.InNode;
import com.samsa.core.node.OutNode;
import com.samsa.core.node.InOutNode;
import com.samsa.core.port.InPort;
import com.samsa.core.port.OutPort;
import com.samsa.node.in.DebugNode;
import com.samsa.node.inout.DelayNode;
import com.samsa.node.inout.FunctionNode;
import com.samsa.node.out.ModbusNode;
import com.samsa.node.out.MqttInNode;

import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.util.HashMap;
import java.util.Map;



@Slf4j
public class FlowLoader {
    private static final ObjectMapper mapper = new ObjectMapper();

    public enum NodeType {
        MQTT_IN("MqttInNode"),
        MODBUS("ModbusNode"),
        DELAY("DelayNode"),
        DEBUG("DebugNode"),
        FUNCTION("FunctionNode");

        private final String typeName;

        NodeType(String typeName) {
            this.typeName = typeName;
        }

        public static NodeType fromString(String type) {
            for (NodeType nodeType : values()) {
                if (nodeType.typeName.equals(type)) {
                    return nodeType;
                }
            }
            throw new IllegalArgumentException("지원되지 않는 노드 타입: " + type);
        }
    }

    public static class FlowLoadException extends RuntimeException {
        public FlowLoadException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    private FlowLoader() {
        throw new UnsupportedOperationException("FlowLoader cannot be instantiated");
    }

    public static Flow loadFlowFromJson(String filePath) {
        try {
            JsonNode root = mapper.readTree(new File(filePath));
            validateFlowStructure(root);
            return createFlow(root);
        } catch (Exception e) {
            log.error("Flow 로딩 중 오류 발생: {}", e.getMessage(), e);
            throw new FlowLoadException("Flow를 로드할 수 없습니다", e);
        }
    }

    private static void validateFlowStructure(JsonNode root) {
        if (!root.has("nodes") || !root.has("connections")) {
            throw new IllegalArgumentException("유효하지 않은 Flow 구조입니다");
        }
    }

    private static Flow createFlow(JsonNode root) throws Exception {
        Flow flow = new Flow();
        Map<String, Node> nodeMap = createNodes(root.get("nodes"), flow);
        createConnections(root.get("connections"), nodeMap);
        return flow;
    }

    private static Map<String, Node> createNodes(JsonNode nodesConfig, Flow flow) {
        Map<String, Node> nodeMap = new HashMap<>();
        Map<String, Integer> nodeTypeCounter = new HashMap<>();
        
        for (JsonNode nodeConfig : nodesConfig) {
            String type = nodeConfig.get("type").asText();
            String id = generateNodeId(type, nodeTypeCounter);
            
            Node node = createNodeWithValidation(nodeConfig);
            nodeMap.put(id, node);
            flow.addNode(node);
        }
        return nodeMap;
    }

    private static String generateNodeId(String type, Map<String, Integer> counter) {
        int count = counter.getOrDefault(type, 0) + 1;
        counter.put(type, count);
        return String.format("%s_%d", type, count);
    }

    private static Node createNodeWithValidation(JsonNode nodeConfig) {
        try {
            String type = nodeConfig.get("type").asText();
            NodeType nodeType = NodeType.fromString(type);
            return createNode(nodeType, nodeConfig.get("properties"));
        } catch (Exception e) {
            log.error("노드 생성 중 오류 발생: {}", e.getMessage(), e);
            throw new FlowLoadException("노드를 생성할 수 없습니다", e);
        }
    }

    private static Node createNode(NodeType type, JsonNode properties) {
        return switch (type) {
            case MQTT_IN -> new MqttInNode(
                    properties.get("broker").asText(),
                    properties.get("clientId").asText(),
                    mapper.convertValue(properties.get("topics"), String[].class)
            );
            case MODBUS -> new ModbusNode(
                properties.get("host").asText(),
                properties.get("port").asInt(),
                properties.get("slaveId").asInt(),
                properties.get("startOffset").asInt(),
                properties.get("numOfRegisters").asInt()
            );

            case DELAY -> new DelayNode(properties.get("delay").asInt());
            case DEBUG -> new DebugNode();
            case FUNCTION -> new FunctionNode(message -> 
                log.info("Payload : {}", message.getPayload())
            );
        };
    }

    private static void createConnections(JsonNode connections, Map<String, Node> nodeMap) {
        for (JsonNode connection : connections) {
            String from = connection.get("from").asText();
            String to = connection.get("to").asText();
            connectNodes(nodeMap.get(from), nodeMap.get(to));
        }
    }

    private static void connectNodes(Node fromNode, Node toNode) {
        OutPort outPort = getOutPort(fromNode);
        InPort inPort = getInPort(toNode);

        if (outPort != null && inPort != null) {
            Pipe pipe = new Pipe();
            outPort.addPipe(pipe);
            inPort.addPipe(pipe);
        } else {
            throw new IllegalStateException(
                String.format("포트 연결 오류: from = %s, to = %s", 
                    fromNode.getId(), toNode.getId())
            );
        }
    }

    private static OutPort getOutPort(Node node) {
        if (node instanceof OutNode outNode) {
            return outNode.getPort();
        } else if (node instanceof InOutNode inOutNode) {
            return inOutNode.getOutPort();
        }
        return null;
    }

    private static InPort getInPort(Node node) {
        if (node instanceof InNode inNode) {
            return inNode.getPort();
        } else if (node instanceof InOutNode inOutNode) {
            return inOutNode.getInPort();
        }
        return null;
    }
}