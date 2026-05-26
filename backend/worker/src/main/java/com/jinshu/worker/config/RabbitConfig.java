package com.jinshu.worker.config;

import org.springframework.amqp.core.Queue;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    @Value("${jinshu.render.queue:jinshu.render}")
    private String renderQueue;

    @Value("${jinshu.render.segment.queue:jinshu.render.segment}")
    private String renderSegmentQueue;

    @Bean
    public Queue renderQueue() {
        return new Queue(renderQueue, true);
    }

    @Bean
    public Queue renderSegmentQueue() {
        return new Queue(renderSegmentQueue, true);
    }
}
