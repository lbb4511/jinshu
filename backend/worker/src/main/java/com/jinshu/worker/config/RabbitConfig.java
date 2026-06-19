package com.jinshu.worker.config;

import com.jinshu.common.mq.MqConstants;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    @Value("${jinshu.export.queue:jinshu.export}")
    private String exportQueue;

    @Value("${jinshu.render.queue:jinshu.render}")
    private String renderQueue;

    @Value("${jinshu.render.segment.queue:jinshu.render.segment}")
    private String renderSegmentQueue;

    @Bean
    public DirectExchange deadLetterExchange() {
        return org.springframework.amqp.core.ExchangeBuilder.directExchange(MqConstants.DLX_EXCHANGE)
                .durable(true)
                .build();
    }

    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder.durable(MqConstants.DLQ_QUEUE).build();
    }

    @Bean
    public Binding deadLetterBinding() {
        return BindingBuilder.bind(deadLetterQueue())
                .to(deadLetterExchange())
                .with(MqConstants.DLX_ROUTING_KEY);
    }

    @Bean
    public Queue exportQueue() {
        return QueueBuilder.durable(exportQueue)
                .withArgument("x-dead-letter-exchange", MqConstants.DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", MqConstants.DLX_ROUTING_KEY)
                .build();
    }

    @Bean
    public Queue renderQueue() {
        return QueueBuilder.durable(renderQueue)
                .withArgument("x-dead-letter-exchange", MqConstants.DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", MqConstants.DLX_ROUTING_KEY)
                .build();
    }

    @Bean
    public Queue renderSegmentQueue() {
        return QueueBuilder.durable(renderSegmentQueue)
                .withArgument("x-dead-letter-exchange", MqConstants.DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", MqConstants.DLX_ROUTING_KEY)
                .build();
    }

    @Bean
    public Jackson2JsonMessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
