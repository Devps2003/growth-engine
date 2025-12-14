package com.growthengine.agent.evaluator;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class EvaluatorAgentApplication {
    
    public static void main(String[] args) {
        SpringApplication app = new SpringApplication(EvaluatorAgentApplication.class);
        app.setWebApplicationType(org.springframework.boot.WebApplicationType.NONE);
        app.run(args);
    }
}