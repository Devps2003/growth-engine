package com.growthengine.agent.researcher;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.web.servlet.DispatcherServletAutoConfiguration;
import org.springframework.boot.autoconfigure.web.servlet.ServletWebServerFactoryAutoConfiguration;

@SpringBootApplication(exclude = {
    ServletWebServerFactoryAutoConfiguration.class,
    DispatcherServletAutoConfiguration.class
})
public class ResearcherAgentApplication {
    
    public static void main(String[] args) {
        SpringApplication.run(ResearcherAgentApplication.class, args);
    }
}