package com.flow;

import com.samsa.core.Flow;
import com.samsa.core.FlowPool;
import com.samsa.core.Pipe;
import com.samsa.node.in.DebugNode;
import com.samsa.node.inout.DelayNode;
import com.samsa.node.out.ModbusNode;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ModbusFlowMain {
    public static void main(String[] args) {
        // FlowPool 생성
        FlowPool flowPool = new FlowPool();

        // Flow 생성
        Flow flow = createModbusDebugFlow();

        // FlowPool에 Flow 추가
        flowPool.addFlow(flow);

        // 전체 FlowPool 실행
        flowPool.run();

        // 프로그램이 바로 종료되지 않도록 대기
        try {
            Thread.sleep(30000); // 30초 동안 실행
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /**
     * MQTT 메시지를 받아서 Debug 노드로 로깅하는 Flow를 생성합니다.
     */
    private static Flow createModbusDebugFlow() {
        Flow flow = new Flow();

        // 1. 노드 생성
        ModbusNode modbusNode = new ModbusNode("192.168.70.213", 502, 1, 0, 1, 0, 0); // Modbus TCP 서버 주소와
                                                                                // 포트
        DelayNode delayNode = new DelayNode(10000); // 1초 지연
        DebugNode debugNode = new DebugNode();

        debugNode.setLogLevel("DEBUG"); // 디버그 레벨 설정

        // 3. 파이프 생성
        Pipe delayToDebug = new Pipe();
        Pipe modbusToPipe = new Pipe();

        // 4. 노드 연결
        // Modbus -> Delay
        modbusNode.getPort().addPipe(modbusToPipe);
        delayNode.getInPort().addPipe(modbusToPipe);

        // Delay -> Debug
        delayNode.getOutPort().addPipe(delayToDebug);
        debugNode.getPort().addPipe(delayToDebug);

        // 5. Flow에 노드 추가
        flow.addNode(modbusNode);
        flow.addNode(delayNode);
        flow.addNode(debugNode);

        return flow;
    }
}
