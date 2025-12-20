package com.growthengine.agent.publisher;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class PublisherAgentApplication {
    
    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(PublisherAgentApplication.class);
        app.setWebApplicationType(org.springframework.boot.WebApplicationType.NONE);
        app.run(args);
    }
}