package com.samsa;

import java.util.HashMap;
import java.util.Map;
import com.samsa.core.Flow;
import com.samsa.core.Pipe;
import com.samsa.node.in.DebugNode;
import com.samsa.node.in.MySqlNode;
import com.samsa.node.inout.FunctionNode;
import com.samsa.node.out.MqttInNode;

public class MqttToMysqlFlow {

    public static Flow createFlow() {
        Flow flow = new Flow();

        MqttInNode mqttInNode =
                new MqttInNode("tcp://192.168.70.203", "dongdong", new String[] {"data/#"});
        DebugNode debugNode = new DebugNode();
        FunctionNode functionNode = new FunctionNode(message -> {
            Map<String, Object> columnMap = new HashMap<>();

            String messageString = message.getPayload().toString();
            String topic = messageString.split(",", 2)[0].split(":")[1];

            if (topic.contains("lora") || topic.contains("power_meter")) {
                return;
            }

            String payload = messageString.split(",", 2)[1].trim().substring(1,
                    messageString.split(",", 2)[1].length() - 3);
            String value = payload.split(",")[1].split(":")[1];
            String timestamp = payload.split(",")[0].split(":")[1];

            String[] topics = topic.split("/");
            columnMap.put("place", topics[6]);
            columnMap.put("spot", topics[10]);
            if (topics.length > 13) {
                columnMap.put("name", topics[14]);
            } else {
                columnMap.put("name", topics[12]);
            }
            columnMap.put("value", Double.parseDouble(value));
            columnMap.put("timestamp", timestamp);

            message.setPayload(columnMap);
        });
        MySqlNode mySqlNode =
                new MySqlNode("com.mysql.cj.jdbc.Driver", "jdbc:mysql://192.168.71.213:3306/IOT",
                        "root", "P@ssw0rd", "INSERT INTO mqtt_data (");

        Pipe mqttInToFunction = new Pipe();
        Pipe functionToDebug = new Pipe();
        Pipe functionToMysql = new Pipe();

        mqttInNode.getPort().addPipe(mqttInToFunction);
        functionNode.getInPort().addPipe(mqttInToFunction);

        functionNode.getOutPort().addPipe(functionToDebug);
        debugNode.getPort().addPipe(functionToDebug);

        functionNode.getOutPort().addPipe(functionToMysql);
        mySqlNode.getPort().addPipe(functionToMysql);

        flow.addNode(mqttInNode);
        flow.addNode(functionNode);
        flow.addNode(debugNode);
        flow.addNode(mySqlNode);

        return flow;
    }
}
