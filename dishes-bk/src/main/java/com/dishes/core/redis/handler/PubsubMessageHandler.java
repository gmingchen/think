package com.dishes.core.redis.handler;

import cn.hutool.core.util.StrUtil;
import com.dishes.common.enums.RedisMQModeEnum;
import com.dishes.core.redis.annotation.MessageHandler;
import com.dishes.core.redis.annotation.MessageListener;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisTemplate;

import java.lang.reflect.Method;
import java.util.HashSet;
import java.util.Set;

/**
 * Pub/sub 处理方式 实现 广播消息
 * @author 好市民
 */
@MessageHandler(value = RedisMQModeEnum.PUBSUB)
public class PubsubMessageHandler extends AbstractMessageHandler {

    public PubsubMessageHandler(RedisTemplate<Object,Object> redisTemplate) {
        super(redisTemplate);
    }

    @Override
    public void invokeMessage(Method method) {
        Set<String> consumers = new HashSet<>();
        MessageListener listener = method.getAnnotation(MessageListener.class);
        String channel = getChannel(listener);
        RedisConnection connection = getConnection();
        connection.subscribe((message, pattern) -> {
            Class<?> declaringClass = method.getDeclaringClass();
            Object bean = applicationContext.getBean(declaringClass);
            byte[] body = message.getBody();
            consumer(method, consumers, bean, body);
        }, channel.getBytes());
    }

    private String getChannel(MessageListener annotation) {
        String value = annotation.value();
        String channel = annotation.channel();
        return StrUtil.isBlank(channel) ? value : channel;
    }
}
