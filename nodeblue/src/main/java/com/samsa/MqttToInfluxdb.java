    package com.samsa;

    import com.samsa.core.Message;
import java.util.List;
import java.util.ArrayList;
import java.util.Map;
import java.util.HashMap;
import java.io.IOException;
import java.io.InputStream;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;


    @Slf4j
    public class MqttToInfluxdb {
        public void handle(Message message) {
            try {
    String rawPayload = message.getPayload().toString();
    String payload = rawPayload.substring(1, rawPayload.length() - 1);
    String topic = payload.split(",", 2)[0];
    String jsonPart = payload.split(",", 2)[1].trim();
    if (topic.contains("lora") || topic.contains("power_meter")) {
        return;
    }
    String[] topicParts = topic.split("/");
    String spot = topicParts[10];
    String measureType = topicParts[topicParts.length - 1];
    String place = topicParts[6];
    String timeStr = jsonPart.split("\\{")[1].split(",")[0].split(":")[1];
    String valueStr = jsonPart.split("value\":")[1].split("}")[0];
    double value = Double.parseDouble(valueStr);
    Map<String, Object> newPayload = new HashMap<>();
    Map<String, String> tags = new HashMap<>();
    Map<String, Object> fields = new HashMap<>();
    tags.put("place", place);
    tags.put("spot", spot);
    fields.put(measureType, value);
    newPayload.put("measurement", "sensor");
    newPayload.put("tags", tags);
    newPayload.put("fields", fields);
    newPayload.put("time", Long.parseLong(timeStr));
    message.setPayload(newPayload);
} catch (Exception e) {
    log.error("Error processing sensor message: {}", e.getMessage());
}
        }
    }
