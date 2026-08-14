package com.campanha.temporeal.infrastructure.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.adapter.MessageListenerAdapter;

@Configuration
public class RedisConfig {

    @Bean
    public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory cf) {
        return new StringRedisTemplate(cf);
    }

    @Bean
    public RedisMessageListenerContainer redisMessageListenerContainer(
            RedisConnectionFactory cf,
            MessageListenerAdapter listenerAdapter) {
        RedisMessageListenerContainer c = new RedisMessageListenerContainer();
        c.setConnectionFactory(cf);
        // Escuta todos os canais de tempo real (sgce:tempo-real:partido:{id})
        c.addMessageListener(listenerAdapter, new PatternTopic("sgce:tempo-real:*"));
        return c;
    }

    @Bean
    public MessageListenerAdapter redisEventListenerAdapter(RedisEventListener listener) {
        MessageListenerAdapter adapter = new MessageListenerAdapter(listener, "onMessage");
        adapter.afterPropertiesSet();
        return adapter;
    }

    @Bean
    public ObjectMapper eventObjectMapper() {
        ObjectMapper m = new ObjectMapper();
        m.findAndRegisterModules();
        return m;
    }
}
