package com.samsa;

import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.paho.client.mqttv3.MqttClient;
import org.eclipse.paho.client.mqttv3.MqttMessage;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.serotonin.modbus4j.ModbusFactory;
import com.serotonin.modbus4j.ModbusMaster;
import com.serotonin.modbus4j.ip.IpParameters;
import com.serotonin.modbus4j.msg.ReadHoldingRegistersRequest;
import com.serotonin.modbus4j.msg.ReadHoldingRegistersResponse;

import lombok.extern.slf4j.Slf4j;

/**
 * Modbus 데이터를 읽고 MQTT 브로커로 전송하는 클래스입니다.
 * 
 * <p>이 클래스는 Modbus 장치의 데이터를 읽어 JSON 형식으로 변환한 후, 
 * MQTT 브로커에 발행하는 역할을 수행합니다. 
 * Modbus 주소와 오프셋 정보를 설정 파일에서 로드하고, 
 * 데이터를 지속적으로 읽어 MQTT로 전송합니다.</p>
 */
@Slf4j
public class ModbusToMqttFlow implements Runnable {

    private Map<Integer, String> addressMap = new HashMap<>(); // Modbus 주소와 장소 매핑
    private Map<Integer, Map<String, Object>> offsetMap = new HashMap<>(); // 오프셋과 메타데이터 매핑

    // Modbus 설정
    private final int slaveId; // Modbus 슬레이브 ID
    private final String modbusHost; // Modbus 장치 IP 주소
    private final int modbusPort; // Modbus TCP 포트

    // MQTT 설정
    private final String mqttBroker; // MQTT 브로커 URL
    private MqttClient mqttClient; // MQTT 클라이언트

    private final IpParameters ipParameters = new IpParameters(); // Modbus IP 설정
    private final ModbusMaster modbusMaster; // Modbus 마스터 객체

    /**
     * 생성자: Modbus와 MQTT 설정을 초기화합니다.
     *
     * @param slaveId     Modbus 슬레이브 ID
     * @param modbusHost  Modbus 장치 IP 주소
     * @param modbusPort  Modbus TCP 포트 (기본값: 502)
     * @param mqttBroker  MQTT 브로커 URL (예: "tcp://192.168.71.213:1883")
     * @throws Exception 초기화 실패 시 예외 발생
     */
    public ModbusToMqttFlow(int slaveId, String modbusHost, int modbusPort, String mqttBroker) throws Exception {
        this.slaveId = slaveId;
        this.modbusHost = modbusHost;
        this.modbusPort = modbusPort;
        this.mqttBroker = mqttBroker;

        ipParameters.setHost(modbusHost);
        ipParameters.setPort(modbusPort);

        modbusMaster = new ModbusFactory().createTcpMaster(ipParameters, true);

        mqttClient = new MqttClient(mqttBroker, MqttClient.generateClientId());
        mqttClient.connect();
    }

    /**
     * 실행 메서드: 지속적으로 Modbus 데이터를 읽고 MQTT로 전송합니다.
     *
     * <p>Modbus 마스터를 초기화하고 설정 파일을 로드한 후,
     * 주기적으로 데이터를 읽어 MQTT로 발행합니다.</p>
     */
    @Override
    public void run() {
        try {
            modbusMaster.init();
            loadConfig();

            while (true) {
                for (Integer address : addressMap.keySet()) {
                    long commonTimestamp = System.currentTimeMillis(); // 공통 타임스탬프 생성

                    for (Integer offset : offsetMap.keySet()) {
                        processModbusData(address, offset, commonTimestamp);
                    }
                }
                Thread.sleep(100); // 폴링 간격 (0.1초)
            }
        } catch (Exception e) {
            log.error("오류 발생", e);
        } finally {
            shutdown();
        }
    }

    /**
     * JSON 파일에서 설정 데이터를 로드합니다.
     *
     * <p>클래스패스에 위치한 `channels.json`과 `channelInfo.json` 파일을 읽어 
     * 주소와 오프셋 정보를 매핑합니다.</p>
     */
    private void loadConfig() {
        ObjectMapper objectMapper = new ObjectMapper();

        try {
            InputStream jsonFileStream = getClass().getClassLoader().getResourceAsStream("channels.json");
            InputStream jsonFileStream2 = getClass().getClassLoader().getResourceAsStream("channelInfo.json");

            if (jsonFileStream == null || jsonFileStream2 == null) {
                log.error("클래스패스에서 JSON 파일을 찾을 수 없습니다.");
                return;
            }

            List<Map<String, Object>> dataList = objectMapper.readValue(jsonFileStream, List.class);
            List<Map<String, Object>> dataList2 = objectMapper.readValue(jsonFileStream2, List.class);

            for (Map<String, Object> item : dataList) {
                int address = (Integer) item.get("address");
                String place = (String) item.get("place");
                addressMap.put(address, place);
            }

            for (Map<String, Object> item : dataList2) {
                int offset = (Integer) item.get("Offset");
                item.remove("Offset");
                offsetMap.put(offset, item);
            }
        } catch (IOException e) {
            log.error("설정 로드 중 오류 발생", e);
        }
    }

    /**
     * 특정 주소와 오프셋에 대한 Modbus 데이터를 처리하고 MQTT로 전송합니다.
     *
     * @param address         Modbus 주소
     * @param offset          오프셋 값
     * @param commonTimestamp 공통 타임스탬프 값
     */
    private void processModbusData(Integer address, Integer offset, long commonTimestamp) {
        try {
            int numberOfRegisters = (int) offsetMap.get(offset).get("Size");
            int scale = (int) offsetMap.get(offset).get("Scale");
            String type = offsetMap.get(offset).get("Type").toString();

            ReadHoldingRegistersRequest request =
                    new ReadHoldingRegistersRequest(slaveId, address + offset, numberOfRegisters);
            ReadHoldingRegistersResponse response =
                    (ReadHoldingRegistersResponse) modbusMaster.send(request);

            if (response.isException()) {
                log.error("Modbus 오류: {}", response.getExceptionMessage());
                return;
            }

            double value = parseModbusResponse(response.getShortData(), type, scale);

            if (value != 0) {
                String topic = "inho/" + addressMap.get(address) + "/" + offsetMap.get(offset).get("Name");
                String payload = createMqttPayload(value, commonTimestamp);
                publishToMqtt(topic, payload);
            }
        } catch (Exception e) {
            log.error("주소 {} 및 오프셋 {} 처리 중 오류 발생: {}", address, offset, e.getMessage());
        }
    }

    /**
     * 응답 데이터를 파싱하여 값을 반환합니다.
     *
     * @param values 응답 데이터 배열
     * @param type   데이터 타입 ("uint16", "int32" 등)
     * @param scale  스케일링 값
     * @return 파싱된 값
     */
    private double parseModbusResponse(short[] values, String type, int scale) {
        double value = 0;

        if (values.length == 1) { // 단일 레지스터 처리
            value = type.equalsIgnoreCase("uint16") ? values[0] & 0xFFFF : values[0];
        } else if (values.length == 2) { // 두 개의 레지스터 처리
            long highPart = ((values[0] & 0xFFFFL) << 16); // 상위 16비트
            long lowPart = (values[1] & 0xFFFFL);          // 하위 16비트
            value = highPart | lowPart;                   // 값 계산
        }

        return value / scale; // 스케일 적용
    }

    /**
     * MQTT 페이로드를 생성합니다.
     *
     * @param value      값 데이터
     * @param timestamp  공통 타임스탬프 값
     * @return JSON 형식의 페이로드 문자열
     */
    private String createMqttPayload(double value, long timestamp) {
        return String.format("{\"time\":%d,\"value\":%.2f}", timestamp, value);
    }

    /**
     * MQTT 메시지를 발행합니다.
     *
     * @param topic   발행할 토픽 이름
     * @param payload 메시지 페이로드(JSON 형식)
     */
    private void publishToMqtt(String topic, String payload) {
        try {
            MqttMessage message = new MqttMessage(payload.getBytes());
            message.setQos(1); // QoS 설정: 최소 한 번 전달 보장

            mqttClient.publish(topic, message);
            log.info("Published to {}: {}", topic, payload);
        } catch (Exception e) {
            log.error("MQTT 발행 오류", e);
        }
    }

    /**
     * 리소스를 정리하고 종료합니다.
     *
     * <p>MQTT 연결 해제 및 Modbus 마스터 객체를 종료합니다.</p>
     */
    private void shutdown() {
        try {
            if (!mqttClient.isConnected()) return;

            mqttClient.disconnect();
            modbusMaster.destroy();
            log.info("MQTT 연결 해제 및 Modbus 마스터 종료 완료.");
        } catch (Exception e) {
            log.error("종료 중 오류 발생", e);
        }
    }

    /**
     * 프로그램의 진입점입니다.
     *
     * <p>ModbusToMqttFlow 인스턴스를 생성하고 실행합니다.</p>
     *
     * @param args 실행 인자 배열
     */
    public static void main(String[] args) throws Exception {
        ModbusToMqttFlow flow =
                new ModbusToMqttFlow(1, "192.168.70.203", 502, "tcp://192.168.71.213:1883");
        new Thread(flow).start();
    }
}
