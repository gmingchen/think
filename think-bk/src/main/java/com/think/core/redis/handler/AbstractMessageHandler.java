package com.think.core.redis.handler;

import cn.hutool.core.util.CharsetUtil;
import cn.hutool.json.JSONUtil;
import com.think.core.redis.pojo.Message;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.core.RedisTemplate;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Set;

/**
 * 处理接口
 * @author 好市民
 */
@Slf4j
public abstract class AbstractMessageHandler implements ApplicationContextAware {

    protected ApplicationContext applicationContext;

    protected RedisTemplate<Object,Object> redisTemplate;

    /**
     * 通过反射 执行 Method 方法
     * @param method 方法
     * @param message 消息
     * @param bean bean 对象 本案例中是 RedisMqConsumer
     */
    protected void invokeMethod(Method method, Message<?> message, Object bean) {
        try {
            method.invoke(bean, message);
        } catch (IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
    }

    protected Message<?> getMessage(byte[] bytes) {
        String s = new String(bytes, CharsetUtil.CHARSET_UTF_8);
        return JSONUtil.toBean(s, Message.class);
    }

    public AbstractMessageHandler(RedisTemplate<Object,Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    /**
     * 获取 RedisConnection
     * @return RedisConnection
     */
    protected RedisConnection getConnection() {
        return redisTemplate.getRequiredConnectionFactory().getConnection();
    }

    /**
     * 执行消息监听逻辑
     * @param method 方法
     */
    public abstract void invokeMessage(Method method);

    @Override
    public void setApplicationContext(@NonNull ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    protected void consumer(Method method, Set<String> consumers, Object bean, byte[] message) {
        Message<?> msg = getMessage(message);
        if (consumers.add(msg.getId())) {
            invokeMethod(method, msg, bean);
        } else {
            log.error("消息已经被消费 {}", msg);
        }
    }
}
