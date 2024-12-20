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
import com.samsa.node.in.InfluxNode;
import com.samsa.node.in.MqttOutNode;
import com.samsa.node.in.MySqlNode;
import com.samsa.node.inout.DelayNode;
import com.samsa.node.inout.FunctionNode;
import com.samsa.node.inout.RangeNode;
import com.samsa.node.inout.ReadFileNode;
import com.samsa.node.inout.WriteFileNode;
import com.samsa.node.out.InjectNode;
import com.samsa.node.out.ModbusNode;
import com.samsa.node.out.MqttInNode;

import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@Slf4j
public class FlowLoaderReflection {
    private static final ObjectMapper mapper = new ObjectMapper();

    /**
     * 노드 타입을 정의하는 열거형입니다.
     */
    public enum NodeType {
        MQTT_IN("MqttInNode"), MODBUS("ModbusNode"), DELAY("DelayNode"), DEBUG(
                "DebugNode"), FUNCTION("FunctionNode"), INJECT(
                        "InjectNode"), INFLUX("InfluxNode"), MQTT_OUT("MqttOutNode"), MYSQL(
                                "MysqlNode"), RANGE("RangeNode"), READ_FILE(
                                        "ReadFileNode"), WRITE_FILE("WriterFileNode");

        private final String typeName;

        NodeType(String typeName) {
            this.typeName = typeName;
        }

        /**
         * 문자열을 노드 타입으로 변환합니다.
         *
         * @param type 문자열로 된 노드 타입
         * @return 노드 타입
         * @throws IllegalArgumentException 지원되지 않는 노드 타입일 경우
         */
        public static NodeType fromString(String type) {
            for (NodeType nodeType : values()) {
                if (nodeType.typeName.equals(type)) {
                    return nodeType;
                }
            }
            throw new IllegalArgumentException("지원되지 않는 노드 타입: " + type);
        }
    }

    /**
     * Flow 파일 로딩 중 발생할 수 있는 예외를 처리하는 클래스입니다.
     */
    public static class FlowLoadException extends RuntimeException {
        public FlowLoadException(String message, Throwable cause) {
            super(message, cause);
        }
    }

    private FlowLoaderReflection() {
        throw new UnsupportedOperationException("FlowLoader는 인스턴스를 생성할 수 없습니다.");
    }

    /**
     * JSON 파일 경로를 받아 Flow 객체를 생성합니다.
     *
     * @param filePath Flow 파일 경로
     * @return 생성된 Flow 객체
     * @throws FlowLoadException Flow 파일 로딩 중 오류가 발생한 경우
     */
    public static Flow loadFlowFromJson(String filePath) {
        try {
            JsonNode root = mapper.readTree(new File(filePath));
            validateFlowStructure(root);
            return createFlow(root);
        } catch (IOException e) {
            log.error("Flow 파일 읽기 중 오류 발생: {}", e.getMessage(), e);
            throw new FlowLoadException("Flow 파일을 읽을 수 없습니다", e);
        } catch (Exception e) {
            log.error("Flow 로딩 중 예기치 않은 오류 발생: {}", e.getMessage(), e);
            throw new FlowLoadException("Flow를 로드할 수 없습니다", e);
        }
    }

    /**
     * Flow 구조를 검증합니다.
     *
     * @param root JSON 노드
     * @throws IllegalArgumentException 'nodes' 또는 'connections' 필드가 없으면 발생
     */
    private static void validateFlowStructure(JsonNode root) {
        if (!root.has("nodes") || !root.has("connections")) {
            throw new IllegalArgumentException("'nodes'와 'connections' 필드가 필요합니다.");
        }
    }

    /**
     * Flow 객체를 생성합니다.
     *
     * @param root JSON 노드
     * @return 생성된 Flow 객체
     * @throws Exception Flow 생성 중 예기치 않은 오류가 발생한 경우
     */
    public static Flow createFlow(JsonNode root) throws Exception {
        try {
            Flow flow = new Flow();
            Map<String, Node> nodeMap = createNodes(root.get("nodes"), flow);
            createConnections(root.get("connections"), nodeMap);
            return flow;
        } catch (IllegalArgumentException e) {
            log.error("Flow 생성 중 오류 발생: {}", e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            log.error("Flow 생성 중 예기치 않은 오류 발생: {}", e.getMessage(), e);
            throw new FlowLoadException("Flow 생성 중 오류가 발생했습니다", e);
        }
    }

    /**
     * JSON에서 노드를 생성합니다.
     *
     * @param nodesConfig JSON에서 읽은 노드 배열
     * @param flow Flow 객체
     * @return 생성된 노드를 담은 맵
     */
    private static Map<String, Node> createNodes(JsonNode nodesConfig, Flow flow) {
        Map<String, Node> nodeMap = new HashMap<>();
        Map<String, Integer> nodeTypeCounter = new HashMap<>();

        for (JsonNode nodeConfig : nodesConfig) {
            try {
                String type = nodeConfig.get("type").asText();
                String id = generateNodeId(type, nodeTypeCounter);

                Node node = createNodeWithValidation(nodeConfig);
                nodeMap.put(id, node);
                flow.addNode(node);
            } catch (Exception e) {
                log.error("노드 생성 중 오류 발생: {}", e.getMessage(), e);
                throw new FlowLoadException("노드 생성 중 오류가 발생했습니다", e);
            }
        }
        return nodeMap;
    }

    /**
     * 노드 ID를 생성합니다.
     *
     * @param type 노드 타입
     * @param counter 노드 타입별 카운터
     * @return 생성된 노드 ID
     */
    private static String generateNodeId(String type, Map<String, Integer> counter) {
        int count = counter.getOrDefault(type, 0) + 1;
        counter.put(type, count);
        return String.format("%s_%d", type, count);
    }

    /**
     * 노드를 생성하고 검증합니다.
     *
     * @param nodeConfig 노드 설정
     * @return 생성된 노드
     * @throws IllegalArgumentException 잘못된 노드 타입일 경우
     */
    private static Node createNodeWithValidation(JsonNode nodeConfig) {
        try {
            String type = nodeConfig.get("type").asText();
            NodeType nodeType = NodeType.fromString(type);
            return createNode(nodeType, nodeConfig.get("properties"));
        } catch (IllegalArgumentException e) {
            log.error("노드 타입 변환 중 오류 발생: {}", e.getMessage(), e);
            throw e;
        } catch (Exception e) {
            log.error("노드 생성 중 예기치 않은 오류 발생: {}", e.getMessage(), e);
            throw new FlowLoadException("노드를 생성할 수 없습니다", e);
        }
    }

    /**
     * 노드를 생성합니다.
     *
     * @param type 노드 타입
     * @param properties 노드 속성
     * @return 생성된 노드
     * @throws IllegalArgumentException 노드 속성 누락 시 발생
     */
    private static Node createNode(NodeType type, JsonNode properties) {
        try {
            return switch (type) {
                case DEBUG -> new DebugNode();
                case INFLUX -> new InfluxNode(properties.get("url").asText(),
                        properties.get("token").asText(), properties.get("org").asText(),
                        properties.get("bucket").asText());
                case MQTT_OUT -> new MqttOutNode(properties.get("broker").asText(),
                        properties.get("clientId").asText());
                case MYSQL -> new MySqlNode(properties.get("driver").asText(),
                        properties.get("url").asText(), properties.get("userId").asText(),
                        properties.get("password").asText(), properties.get("sql").asText());
                case DELAY -> new DelayNode(properties.get("delay").asInt());
                case FUNCTION -> new FunctionNode(properties.get("className").asText(),
                        properties.get("code").asText());
                case RANGE -> new RangeNode(properties.get("inputMin").asDouble(),
                        properties.get("inputMax").asDouble(),
                        properties.get("outputMin").asDouble(),
                        properties.get("outputMax").asDouble(),
                        properties.get("constrainToTarget").asBoolean());
                case READ_FILE -> new ReadFileNode(properties.get("path").asText(),
                        properties.get("readAllLine").asBoolean());
                case WRITE_FILE -> new WriteFileNode(properties.get("path").asText(),
                        properties.get("append").asBoolean());
                case INJECT -> new InjectNode(properties.get("payload").asText());
                case MODBUS -> new ModbusNode(properties.get("host").asText(),
                        properties.get("port").asInt(), properties.get("slaveId").asInt(),
                        properties.get("startOffset").asInt(),
                        properties.get("offsetInterval").asInt(),
                        properties.get("maxOffset").asInt(),
                        properties.get("numOfRegisters").asInt());
                case MQTT_IN -> new MqttInNode(properties.get("broker").asText(),
                        properties.get("clientId").asText(),
                        mapper.convertValue(properties.get("topics"), String[].class));
            };
        } catch (NullPointerException e) {
            log.error("노드 생성 중 필수 속성 누락: {}", e.getMessage(), e);
            throw new IllegalArgumentException("노드 생성 시 필수 속성이 누락되었습니다", e);
        } catch (Exception e) {
            log.error("노드 생성 중 예기치 않은 오류 발생: {}", e.getMessage(), e);
            throw new FlowLoadException("노드를 생성할 수 없습니다", e);
        }
    }

    /**
     * 노드들 간의 연결을 생성합니다.
     *
     * @param connections 연결 설정
     * @param nodeMap 노드 맵
     */
    private static void createConnections(JsonNode connections, Map<String, Node> nodeMap) {
        for (JsonNode connection : connections) {
            try {
                String from = connection.get("from").asText();
                String to = connection.get("to").asText();
                connectNodes(nodeMap.get(from), nodeMap.get(to));
            } catch (NullPointerException e) {
                log.error("연결 생성 중 노드 정보 누락: {}", e.getMessage(), e);
                throw new IllegalArgumentException("연결 생성 중 필요한 노드 정보가 누락되었습니다", e);
            } catch (Exception e) {
                log.error("연결 생성 중 예기치 않은 오류 발생: {}", e.getMessage(), e);
                throw new FlowLoadException("연결 생성 중 오류가 발생했습니다", e);
            }
        }
    }

    /**
     * 노드들 간의 포트를 연결합니다.
     *
     * @param fromNode 출발 노드
     * @param toNode 도착 노드
     * @throws IllegalStateException 포트 연결 오류가 발생한 경우
     */
    private static void connectNodes(Node fromNode, Node toNode) {
        try {
            OutPort outPort = getOutPort(fromNode);
            InPort inPort = getInPort(toNode);

            if (outPort != null && inPort != null) {
                Pipe pipe = new Pipe();
                outPort.addPipe(pipe);
                inPort.addPipe(pipe);
            } else {
                throw new IllegalStateException("포트 연결에 실패했습니다");
            }
        } catch (IllegalStateException e) {
            log.error("노드 간 연결 실패: {}", e.getMessage(), e);
            throw e;
        }
    }

    /**
     * 노드의 출력 포트를 가져옵니다.
     *
     * @param node 노드 객체
     * @return 출력 포트
     * @throws IllegalStateException 출력 포트가 없으면 발생
     */
    private static OutPort getOutPort(Node node) {
        if (node instanceof OutNode) {
            return ((OutNode) node).getPort();
        } else if (node instanceof InOutNode) {
            return ((InOutNode) node).getOutPort();
        }
        throw new IllegalStateException("출력 포트를 찾을 수 없습니다");
    }

    /**
     * 노드의 입력 포트를 가져옵니다.
     *
     * @param node 노드 객체
     * @return 입력 포트
     * @throws IllegalStateException 입력 포트가 없으면 발생
     */
    private static InPort getInPort(Node node) {
        if (node instanceof InNode) {
            return ((InNode) node).getPort();
        } else if (node instanceof InOutNode) {
            return ((InOutNode) node).getInPort();
        }
        throw new IllegalStateException("입력 포트를 찾을 수 없습니다");
    }
}
