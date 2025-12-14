package com.growthengine.agent.writer;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.reactive.context.ReactiveWebServerApplicationContext;

@SpringBootApplication
public class WriterAgentApplication {
    
    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(WriterAgentApplication.class);
        app.setWebApplicationType(org.springframework.boot.WebApplicationType.NONE);
        app.run(args);
    }
}

