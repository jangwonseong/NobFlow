package com.flow;

import java.io.File;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.samsa.core.Flow;
import com.samsa.core.node.InNode;
import com.samsa.core.node.InOutNode;
import com.samsa.core.node.Node;
import com.samsa.core.node.OutNode;

public class FlowWriter {
    public static void writeFlowToJson(Flow flow, String filePath) {
        try {
            ObjectMapper mapper = new ObjectMapper();
            ObjectNode root = mapper.createObjectNode();
            
            var nodesArray = root.putArray("nodes");
            for (Node node : flow.getNodes()) {
                ObjectNode nodeObject = nodesArray.addObject();
                nodeObject.put("type", node.getClass().getSimpleName());

                // Check node type and cast appropriately
                if (node instanceof InOutNode) {
                    InOutNode inOutNode = (InOutNode) node;
                    nodeObject.put("inPort", inOutNode.getInPort().toString());
                    nodeObject.put("outPort", inOutNode.getOutPort().toString());
                } else if (node instanceof InNode) {
                    InNode inNode = (InNode) node;
                    nodeObject.put("inPort", inNode.getPort().toString());
                    nodeObject.put("outPort", "null");
                } else if (node instanceof OutNode) {
                    OutNode outNode = (OutNode) node;
                    nodeObject.put("inPort", "null");
                    nodeObject.put("outPort", outNode.getPort().toString());
                }
            }

            mapper.writerWithDefaultPrettyPrinter().writeValue(new File(filePath), root);
            System.out.println("Flow saved to JSON: " + filePath);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
