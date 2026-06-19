package com.jinshu.api.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jinshu.api.service.ExportProgressService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;

/**
 * Redis 消息监听配置。
 *
 * 订阅 Worker 发布的导出进度事件通道，并转发给 SSE 服务推送给客户端。
 */
@Slf4j
@Configuration
@RequiredArgsConstructor
@ConditionalOnBean(RedisConnectionFactory.class)
public class RedisMessageConfig {

    private static final String PROGRESS_EVENT_CHANNEL = "jinshu:export:progress:events";

    private final ExportProgressService exportProgressService;
    private final ObjectMapper objectMapper;

    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(RedisConnectionFactory connectionFactory) {
        RedisMessageListenerContainer container = new RedisMessageListenerContainer();
        container.setConnectionFactory(connectionFactory);
        container.addMessageListener(messageListener(), new PatternTopic(PROGRESS_EVENT_CHANNEL));
        return container;
    }

    @Bean
    public MessageListenerAdapter messageListener() {
        return new MessageListenerAdapter(this, "onMessage");
    }

    public void onMessage(String message, String pattern) {
        try {
            exportProgressService.handleProgressMessage(message);
        } catch (Exception e) {
            log.warn("Failed to handle export progress message from channel {}: {}", pattern, message, e);
        }
    }
}
