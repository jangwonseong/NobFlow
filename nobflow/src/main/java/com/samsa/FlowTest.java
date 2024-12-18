package com.samsa;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import com.samsa.core.Flow;
import com.samsa.core.FlowPool;

import lombok.extern.slf4j.Slf4j;



@SpringBootApplication
@Slf4j
public class FlowTest {
//     public static void main(String[] args) {
//         SpringApplication.run(FlowTest.class, args);
//     }
// }

    
    public static void main(String[] args) {
        try {
            FlowPool flowPool = new FlowPool();
            Flow flow = FlowLoaderReflection.loadFlowFromJson(
                    "/home/nhnacademy/Desktop/NobFlow/nobflow/src/main/resources/flow.json"
            );
            flowPool.addFlow(flow);
            flowPool.run();
            log.info("Flow execution completed!");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}