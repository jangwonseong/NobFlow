package com.samsa;

import java.util.Properties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import lombok.extern.slf4j.Slf4j;


/**
 * NobFlow 애플리케이션의 메인 클래스
 * 스프링 부트 애플리케이션의 시작점으로 서버 설정과 실행을 담당
 */
@SpringBootApplication
@Slf4j
public class NobflowApplication {
    
    public static void main(String[] args) {
        SpringApplication application = new SpringApplication(NobflowApplication.class);
        
        Properties defaultProperties = new Properties();
        defaultProperties.setProperty("server.port", "8081");
        defaultProperties.setProperty("logging.config", "classpath:logback-spring.xml");
        defaultProperties.setProperty("spring.web.resources.static-locations", "classpath:/static/");
        defaultProperties.setProperty("server.tomcat.max-threads", "200");
        
        application.setDefaultProperties(defaultProperties);
        
    /**
     * 애플리케이션 메인 메서드
     * 서버 포트, 로깅 설정, 정적 리소스 위치, 스레드 풀 등 기본 설정 후 서버 실행
     * 
     * @param args 실행 인자
     */
        try {
            application.run(args);
            log.info("NobFlow 애플리케이션이 성공적으로 시작되었습니다.");
            log.info("서버 주소: http://localhost:8081");
        } catch (Exception e) {
            log.error("애플리케이션 시작 중 오류 발생: " + e.getMessage());
            log.error("애플리케이션 종료");
            System.exit(1);
        }
    }
}
