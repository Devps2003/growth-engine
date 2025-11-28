package com.growthengine.agent.seo.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {
    
    // Get queue name from application.yml
    @Value("${seo.queue.name}")
    private String queueName;
    
    /**
     * Declares the SEO queue in RabbitMQ.
     * This ensures the queue exists when the application starts.
     */
    @Bean
    public Queue seoQueue() {
        return new Queue(queueName, true); // true = durable queue
    }
    
    /**
     * Provides ObjectMapper for JSON processing.
     * Since we don't use spring-boot-starter-web, we need to provide this explicitly.
     */
    @Bean
    public ObjectMapper objectMapper() {
        return new ObjectMapper();
    }
    
    /**
     * Configures JSON message converter for RabbitMQ.
     * This converts Java objects to JSON when sending/receiving messages.
     */
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
    
    /**
     * Configures RabbitMQ listener container factory.
     * This is needed for @RabbitListener to work properly with JSON messages.
     */
    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            MessageConverter jsonMessageConverter) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(jsonMessageConverter);
        return factory;
    }
}