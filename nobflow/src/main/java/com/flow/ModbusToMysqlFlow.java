package com.flow;

import java.util.HashMap;
import java.util.Map;
import com.samsa.core.Flow;
import com.samsa.core.Pipe;
import com.samsa.node.in.DebugNode;
import com.samsa.node.in.MySqlNode;
import com.samsa.node.inout.FunctionNode;
import com.samsa.node.out.MqttInNode;

public class ModbusToMysqlFlow {

    static Flow createFlow() {
        // 노드 생성
        MqttInNode mqttInNode =
                new MqttInNode("tcp://192.168.71.213:1883", "ghkdhgk", new String[] {"inho/#"});
        DebugNode debugNode = new DebugNode();

        // MySQL 연결을 처리할 FunctionNode 생성
        FunctionNode functionNode = new FunctionNode(message -> {
            Map<String, Object> columnMap = new HashMap<>();

            String messageString = message.getPayload().toString();
            String topic = messageString.split(",", 2)[0].split(":")[1];

            String payload = messageString.split(",", 2)[1].trim().substring(1,
                    messageString.split(",", 2)[1].length() - 3);
            String value = payload.split(",")[1].split(":")[1];
            String timestamp = payload.split(",")[0].split(":")[1];

            String[] topics = topic.split("/");
            columnMap.put("place", topics[1]);
            columnMap.put("name", topics[2]);
            columnMap.put("value", value);
            columnMap.put("timestamp", timestamp);
            message.setPayload(columnMap);
        });

        MySqlNode mySqlNode =
                new MySqlNode("com.mysql.cj.jdbc.Driver", "jdbc:mysql://192.168.71.213:3306/IOT",
                        "root", "P@ssw0rd", "INSERT INTO modbus_data (");

        // 파이프 생성 및 연결
        Pipe mqttInToFunction = new Pipe();
        Pipe functionToDebug = new Pipe();
        Pipe functionToMysql = new Pipe();

        mqttInNode.getPort().addPipe(mqttInToFunction);
        functionNode.getInPort().addPipe(mqttInToFunction);

        functionNode.getOutPort().addPipe(functionToDebug);
        debugNode.getPort().addPipe(functionToDebug);

        functionNode.getOutPort().addPipe(functionToMysql);
        mySqlNode.getPort().addPipe(functionToMysql);

        // Flow 설정
        Flow flow = new Flow();
        flow.addNode(mqttInNode);
        flow.addNode(mySqlNode);
        flow.addNode(functionNode);
        flow.addNode(debugNode);

        return flow;
    }
}
