package com.samsa;

import java.util.Properties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class NobflowApplication {
    
    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(NobflowApplication.class);
        
        Properties defaultProperties = new Properties();
        defaultProperties.setProperty("server.port", "8081");
        defaultProperties.setProperty("logging.config", "classpath:logback-spring.xml");
        defaultProperties.setProperty("spring.web.resources.static-locations", "classpath:/static/");
        defaultProperties.setProperty("server.tomcat.max-threads", "200");
        
        application.setDefaultProperties(defaultProperties);
        
        try {
            application.run(args);
            System.out.println("NobFlow 애플리케이션이 성공적으로 시작되었습니다.");
            System.out.println("서버 주소: http://localhost:8081");
        } catch (Exception e) {
            System.err.println("애플리케이션 시작 중 오류 발생: " + e.getMessage());
            System.exit(1);
        }
    }
}
