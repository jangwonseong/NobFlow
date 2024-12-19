package com.samsa;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.flow.FlowLoader.FlowLoadException;
import com.samsa.core.Flow;
import com.samsa.core.Pipe;
import com.samsa.core.node.Node;
import com.samsa.core.node.InNode;
import com.samsa.core.node.OutNode;
import com.samsa.core.node.InOutNode;
import com.samsa.core.port.InPort;
import com.samsa.core.port.OutPort;

import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.reflections.Reflections;

import com.samsa.annotation.NodeType;

/**
 * Flow 구성을 JSON 파일로부터 로드하고 생성하는 유틸리티 클래스
 * 리플렉션을 사용하여 동적으로 Node 객체를 생성하고 연결
 */

@Slf4j
public class FlowLoaderReflection {
    /** JSON 파싱을 위한 ObjectMapper */
    private static final ObjectMapper mapper = new ObjectMapper();
    
    /** Node 타입별 패키지 경로 매핑 */
    private static final Map<String, String> NODE_PACKAGE_MAP = loadPackageMapping();
    
    /** Node 클래스 캐시 */
    private static final Map<String, Class<?>> NODE_CACHE = new HashMap<>();


    /**
     * 인스턴스화 방지를 위한 private 생성자
     */
    private FlowLoaderReflection() {
        throw new UnsupportedOperationException("FlowLoader cannot be instantiated");
    }

    /**
     * JSON 파일로부터 Flow를 로드
     * @param filePath Flow JSON 파일 경로
     * @return 생성된 Flow 객체
     */
    public static Flow loadFlowFromJson(String filePath) {
        try {
            JsonNode root = mapper.readTree(new File(filePath));
            validateFlowStructure(root);
            return createFlow(root);
        } catch (Exception e) {
            log.error("Flow 로딩 중 오류 발생: {}", e.getMessage(), e);
            throw new UnsupportedOperationException("FlowLoader cannot be instantiated");
        }
    }

    /**
     * 구조 검증 메서드
     * @param root Flow 구성이 담긴 JSON 노드
     */
    private static void validateFlowStructure(JsonNode root) {
        if (!root.has("nodes") || !root.has("connections")) {
            throw new IllegalArgumentException("유효하지 않은 Flow 구조입니다");
        }
    }
    
    /**
     * JSON 노드로부터 Flow 생성
     * @param root Flow 구성이 담긴 JSON 노드
     * @return 생성된 Flow 객체
     */
    public static Flow createFlow(JsonNode root) throws Exception {
        validateFlowStructure(root);
        Flow flow = new Flow();
        Map<String, Node> nodeMap = createNodes(root.get("nodes"), flow);
        createConnections(root.get("connections"), nodeMap);
        return flow;
    }

    /**
     * 노드 생성 메서드
     * @param nodesConfig 노드 구성이 담긴 JSON 노드
     * @param flow Flow 객체
     * @return 생성된 노드 맵
     */
    private static Map<String, Node> createNodes(JsonNode nodesConfig, Flow flow) {
        Map<String, Node> nodeMap = new HashMap<>();
        for (JsonNode nodeConfig : nodesConfig) {
            Node node = createNodeWithReflection(nodeConfig);
            nodeMap.put(nodeConfig.get("id").asText(), node);
            flow.addNode(node);
        }
        return nodeMap;
    }

    /**
     * 리플렉션을 사용하여 노드 생성
     * @param nodeConfig 노드 구성이 담긴 JSON 노드
     * @return 생성된 노드
     */
    private static Node createNodeWithReflection(JsonNode nodeConfig) {
        try {
            String type = nodeConfig.get("type").asText();
            Class<?> nodeClass = getNodeClass(type);
            JsonNode properties = nodeConfig.get("properties");
    
            // Add validation before conversion
            if (!Node.class.isAssignableFrom(nodeClass)) {
                throw new UnsupportedOperationException("FlowLoader cannot be instantiated");
            }
    
            return (Node) mapper.convertValue(properties, nodeClass);
        } catch (Exception e) {
            throw new FlowLoadException("Node creation failed: " + e.getMessage(), e);
        }
    }

    /**
     * 노드 클래스 캐시에서 노드 클래스를 가져오는 메서드
     * @param type 노드 타입
     * @return 노드 클래스
     */
    private static Class<?> getNodeClass(String type) throws ClassNotFoundException {
        if (NODE_CACHE.containsKey(type)) {
            return NODE_CACHE.get(type);
        }

        String packagePath = NODE_PACKAGE_MAP.getOrDefault(type, "com.samsa.node.default");
        String className = String.format("%s.%s", packagePath, type);
        Class<?> nodeClass = Class.forName(className);
        NODE_CACHE.put(type, nodeClass);
        return nodeClass;
    }

    /**
     * 노드 패키지 매핑 로드
     * @return 노드 타입별 패키지 매핑
     */
    private static Map<String, String> loadPackageMapping() {
        try {
            // 클래스패스에서 @NodeType 어노테이션이 있는 클래스들을 스캔
            Reflections reflections = new Reflections("com.samsa.node");
            Set<Class<?>> nodeClasses = reflections.getTypesAnnotatedWith(NodeType.class);
            
            return nodeClasses.stream()
                .filter(cls -> cls.isAnnotationPresent(NodeType.class))
                .collect(Collectors.toMap(
                    cls -> cls.getAnnotation(NodeType.class).value(),
                    Class::getPackageName,
                    (existing, replacement) -> existing // 중복 시 첫 번째 값 유지
                ));
        } catch (Exception e) {
            log.error("노드 패키지 매핑 로드 중 오류 발생", e);
            return new HashMap<>();
        }
    }
    

    /**
     * 노드 연결 생성
     * @param connections 연결 구성이 담긴 JSON 노드
     * @param nodeMap 생성된 노드 맵
     */
    private static void createConnections(JsonNode connections, Map<String, Node> nodeMap) {
        for (JsonNode connection : connections) {
            String from = connection.get("from").asText();
            String to = connection.get("to").asText();

            try {
                connectNodes(nodeMap.get(from), nodeMap.get(to));
            } catch (Exception e) {
                log.warn("노드 연결 실패: from = {}, to = {}. 오류: {}", from, to, e.getMessage());
            }
        }
    }

    /**
     * 노드 연결 생성
     * @param fromNode 소스 노드
     * @param toNode 대상 노드
     */
    private static void connectNodes(Node fromNode, Node toNode) {
        if (fromNode == null || toNode == null) {
            throw new IllegalArgumentException("Nodes cannot be null");
        }
    
        OutPort outPort = getOutPort(fromNode);
        InPort inPort = getInPort(toNode);
    
        if (outPort == null || inPort == null) {
            throw new IllegalStateException(
                String.format("Invalid port configuration: from=%s, to=%s", 
                    fromNode.getId(), toNode.getId())
            );
        }
    
        Pipe pipe = new Pipe();
        outPort.addPipe(pipe);
        inPort.addPipe(pipe);
    }
    
    /**
     * 소스 노드의 출력 포트를 가져오는 메서드
     * @param node 노드
     * @return 출력 포트
     */
    private static OutPort getOutPort(Node node) {
        if (node instanceof OutNode outNode) {
            return outNode.getPort();
        } else if (node instanceof InOutNode inOutNode) {
            return inOutNode.getOutPort();
        }
        return null;
    }

    /**
     * 대상 노드의 입력 포트를 가져오는 메서드
     * @param node 노드
     * @return 입력 포트
     */
    private static InPort getInPort(Node node) {
        if (node instanceof InNode inNode) {
            return inNode.getPort();
        } else if (node instanceof InOutNode inOutNode) {
            return inOutNode.getInPort();
        }
        return null;
    }
}
                          