package com.think.core.redis.config;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.jsontype.impl.LaissezFaireSubTypeValidator;
import com.fasterxml.jackson.datatype.jdk8.Jdk8Module;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fasterxml.jackson.module.paramnames.ParameterNamesModule;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.connection.stream.ObjectRecord;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.StringRedisSerializer;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.time.Duration;

/**
 * redis 配置
 * @author 好市民
 */
@Slf4j
@Configuration
public class RedisConfig {

    /**
     * 创建 RedisTemplate Bean，使用 JSON 序列化方式
     */
    @Bean
    @SuppressWarnings("all")
    public RedisTemplate<Object, Object> redisTemplate(RedisConnectionFactory factory) {
        // 创建 RedisTemplate 对象
        RedisTemplate<Object, Object> redisTemplate = new RedisTemplate<>();
        // LocalDateTime 序列化方式
        // 使用 String 序列化方式，序列化 KEY
        redisTemplate.setKeySerializer(RedisSerializer.string());
        redisTemplate.setHashKeySerializer(RedisSerializer.string());
        // 序列化 LocalDateTime
        Jackson2JsonRedisSerializer<Object> serializer = new Jackson2JsonRedisSerializer<>(Object.class);
        ObjectMapper objectMapper = new ObjectMapper()
                .registerModule(new ParameterNamesModule())
                .registerModule(new Jdk8Module())
                .registerModule(new JavaTimeModule());
        objectMapper.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        objectMapper.activateDefaultTyping(LaissezFaireSubTypeValidator.instance, ObjectMapper.DefaultTyping.NON_FINAL);
        serializer.setObjectMapper(objectMapper);
        redisTemplate.setValueSerializer(serializer);
        redisTemplate.setHashValueSerializer(RedisSerializer.string());
        // 设置 RedisConnection 工厂
        redisTemplate.setConnectionFactory(factory);

        return redisTemplate;
    }

    /**
     * 获取 StreamMessageListenerContainer
     * @param executor
     * @param redisTemplate
     * @return
     */
    @Bean
    @SuppressWarnings("all")
    public StreamMessageListenerContainer<String, ObjectRecord<String, String>> stringMapRecordStreamMessageListenerContainer(ThreadPoolTaskExecutor executor, RedisTemplate<Object, Object> redisTemplate) {
        StreamMessageListenerContainer.StreamMessageListenerContainerOptions<String, ObjectRecord<String, String>> options =
                StreamMessageListenerContainer.StreamMessageListenerContainerOptions
                        .builder()
                        // 拉取消息超时时间
                        .pollTimeout(Duration.ofSeconds(5))
                        // 批量抓取消息
                        .batchSize(1)
                        // 序列化
                        .serializer(new StringRedisSerializer())
                        // 传递的数据类型
                        .targetType(String.class)
                        .executor(executor)
                        .build();
        RedisConnectionFactory connectionFactory = redisTemplate.getConnectionFactory();
        assert connectionFactory != null;
        StreamMessageListenerContainer<String, ObjectRecord<String, String>> container = StreamMessageListenerContainer
                .create(connectionFactory, options);
        container.start();
        return container;
    }

}
