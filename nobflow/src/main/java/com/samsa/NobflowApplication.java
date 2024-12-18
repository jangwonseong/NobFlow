package com.samsa;

import java.util.Properties;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class NobflowApplication {
    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(NobflowApplication.class);
        Properties properties = new Properties();
        properties.setProperty("server.port", "8081"); // Use a different port
        application.setDefaultProperties(properties);
        application.run(args);
    }
}