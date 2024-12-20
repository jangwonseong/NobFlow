package com.flow;

import com.samsa.core.Flow;
import com.samsa.core.FlowPool;
import com.samsa.core.Pipe;
import com.samsa.node.in.InfluxNode;
import com.samsa.node.inout.DelayNode;
import com.samsa.node.out.ModbusNode;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class InfluxFlowMain {
    public static void main(String[] args) {
        // FlowPool 생성
        FlowPool flowPool = new FlowPool();

        // Flow 생성
        Flow flow = createModbusInfluxFlow();

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
     * Modbus 메시지를 받아서 InfluxDB로 저장하는 Flow를 생성합니다.
     */
    private static Flow createModbusInfluxFlow() {
        Flow flow = new Flow();

        // 1. 노드 생성
        ModbusNode modbusNode = new ModbusNode("192.168.70.203", 502, 1, 0, 1, 0, 0); // Modbus TCP 서버 주소와 포트
        DelayNode delayNode = new DelayNode(10000); // 10초 지연
        InfluxNode influxNode = new InfluxNode("http://192.168.71.221:8086", "7BuWmxpqHzyY7dN6jZiysF8SvdXEdnccAq9Uq23Mr7uL9NGc2tkZKrIkiTmaD4QtXY7NxaJSG8hbqs_kvu1kZQ==", "seong", "nhnacademy"); // InfluxDB 설정

        // 2. 파이프 생성
        Pipe delayToInflux = new Pipe();
        Pipe modbusToPipe = new Pipe();

        // 3. 노드 연결
        // Modbus -> Delay
        modbusNode.getPort().addPipe(modbusToPipe);
        delayNode.getInPort().addPipe(modbusToPipe);

        // Delay -> Influx
        delayNode.getOutPort().addPipe(delayToInflux);
        influxNode.getPort().addPipe(delayToInflux);

        // 4. Flow에 노드 추가
        flow.addNode(modbusNode);
        flow.addNode(delayNode);
        flow.addNode(influxNode);

        return flow;
    }
}