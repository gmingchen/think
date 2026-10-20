package com.think.core.redis.handler;

import cn.hutool.json.JSONUtil;
import com.think.common.enums.RedisMQModeEnum;
import com.think.core.redis.annotation.MessageHandler;
import com.think.core.redis.annotation.MessageListener;
import com.think.core.redis.pojo.Message;
import com.think.core.redis.utils.RedisStreamUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.stream.*;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.StreamOperations;
import org.springframework.data.redis.stream.StreamMessageListenerContainer;

import org.springframework.beans.factory.annotation.Autowired;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * Stream方式 实现消息队列
 * @author 好市民
 */
@MessageHandler(value = RedisMQModeEnum.STREAM)
@Slf4j
public class StreamMessageHandler extends AbstractMessageHandler {
    @Autowired
    StreamMessageListenerContainer<String, ObjectRecord<String, String>> redisListenerContainer;

    @Autowired
    private RedisStreamUtils redisStreamUtils;

    public StreamMessageHandler(RedisTemplate<Object, Object> redisTemplate) {
        super(redisTemplate);
    }

    @Override
    public synchronized void invokeMessage(Method method) {
        // 获取注解中的相关信息
        MessageListener annotation = method.getAnnotation(MessageListener.class);
        String streamKey = annotation.streamKey();
        String consumerGroup = annotation.consumerGroup();
        String consumerName = annotation.consumerName();
        boolean pending = annotation.pending();
        boolean ack = annotation.ack();

        // 检测组是否存在
        this.checkAndCreatGroup(streamKey, consumerGroup);

        // 获取 StreamOffset
        StreamOffset<String> offset = this.getOffset(streamKey, pending);

        // 创建消费者
        Consumer consumer = Consumer.from(consumerGroup, consumerName);

        StreamMessageListenerContainer.StreamReadRequest<String> streamReadRequest =
                StreamMessageListenerContainer
                        .StreamReadRequest.builder(offset)
                        .errorHandler((error) -> log.error("[RedisMQ-redis stream 异常：{}]", error.getMessage()))
                        .cancelOnError(e -> false)
                        .consumer(consumer)
                        //关闭自动ack确认
                        .autoAcknowledge(false)
                        .build();

        // 指定消费者对象注册到容器中去
        redisListenerContainer.register(streamReadRequest, (record) -> {
            Class<?> declaringClass = method.getDeclaringClass();
            Object bean = applicationContext.getBean(declaringClass);
            Message<?> message = JSONUtil.toBean(record.getValue(), Message.class);
            String jsonStr = JSONUtil.toJsonStr(message.getContent());
            jsonStr = "{}".equals(jsonStr) ? null : jsonStr;
            log.debug("[RedisMQ-【{}】【{}】【{}】 消费消息：{}]", streamKey, consumerGroup, consumerName, jsonStr);
            try {
                method.invoke(bean, record, jsonStr);
                // 判断是否ack
                if (ack) {
                    redisStreamUtils.ack(consumerGroup, record);
                    redisStreamUtils.delete(streamKey, record.getId().getValue());
                }
            } catch (IllegalAccessException | InvocationTargetException e) {
                e.printStackTrace();
            }
        });
    }

    private StreamOffset<String> getOffset(String streamKey, boolean pending) {
        if (pending) {
            // 获取尚未 ack 的消息
            return StreamOffset.create(streamKey, ReadOffset.from("0"));
        }
        // 指定消费最新的消息
        return StreamOffset.create(streamKey, ReadOffset.lastConsumed());
    }

    /**
     * 校验消费组是否存在，不存在则创建，否则会出现异常
     * Error in execution; nested exception is io.lettuce.core.RedisCommandExecutionException: NOGROUP No such key 'streamKey' or consumer group 'consumerGroup' in XREADGROUP with GROUP option
     *
     * @param streamKey     streamKey
     * @param consumerGroup consumerGroup
     */
    private void checkAndCreatGroup(String streamKey, String consumerGroup) {
        if (Boolean.TRUE.equals(redisTemplate.hasKey(streamKey))) {
            StreamOperations<Object, Object, Object> streamOperations = redisTemplate.opsForStream();
            StreamInfo.XInfoGroups groups = streamOperations.groups(streamKey);
            if (groups.isEmpty() || groups.stream().noneMatch(data -> consumerGroup.equals(data.groupName()))) {
                creatGroup(streamKey, consumerGroup);
            } else {
                groups.stream().forEach(g -> {
                    StreamInfo.XInfoConsumers consumers = streamOperations.consumers(streamKey, g.groupName());
                });
            }
        } else {
            creatGroup(streamKey, consumerGroup);
        }
    }

    private void creatGroup(String key, String group) {
        StreamOperations<Object, Object, Object> streamOperations = redisTemplate.opsForStream();
        streamOperations.createGroup(key, group);
        log.debug("[RedisMQ-创建组成功：{}]", group);
    }

}
