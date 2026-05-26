package com.jinshu.api.config;

import org.springframework.amqp.core.Queue;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    @Value("${jinshu.export.queue:jinshu.export}")
    private String exportQueue;

    @Value("${jinshu.import.queue:P4}")
    private String importQueue;

    @Bean
    public Queue exportQueue() {
        return new Queue(exportQueue, true);
    }

    @Bean
    public Queue importQueue() {
        return new Queue(importQueue, true);
    }
}
