package com.jinshu.api.config;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    @Value("${jinshu.export.queue:jinshu.export}")
    private String exportQueue;

    @Value("${jinshu.import.queue:jinshu.import.task}")
    private String importQueue;

    @Bean
    public Queue exportQueue() {
        return new Queue(exportQueue, true);
    }

    @Bean
    public Queue importQueue() {
        return new Queue(importQueue, true);
    }

    @Bean
    public Jackson2JsonMessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
