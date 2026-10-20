package com.think.core.redis.context;

import cn.hutool.core.util.ArrayUtil;
import com.think.common.enums.RedisMQModeEnum;
import com.think.common.utils.ApplicationContextUtils;
import com.think.core.redis.annotation.MessageConsumer;
import com.think.core.redis.annotation.MessageHandler;
import com.think.core.redis.annotation.MessageListener;
import com.think.core.redis.handler.AbstractMessageHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 启动配置，在项目启动时自动运行，注册消费者
 * @author 好市民
 */
@Component
@Slf4j
public class MessageConsumerStater implements ApplicationRunner {

    public MessageConsumerStater() {}

    /**
     * 项目启动时，获取所有的 标注 MessageConsumer 注解的方法，开启消息监听逻辑
     * @param args ApplicationArguments
     */
    @Override
    public void run(ApplicationArguments args) {
        Map<RedisMQModeEnum, AbstractMessageHandler> invokers = getInvokers();
        ApplicationContextUtils.applicationContext.getBeansWithAnnotation(MessageConsumer.class).values().parallelStream().forEach(consumer -> {
            Method[] methods = consumer.getClass().getMethods();
            if (ArrayUtil.isNotEmpty(methods)) {
                Arrays.stream(methods).parallel().forEach(method -> startMessageListener(method, invokers));
            }
        });
    }

    /**
     * 找到对应的处理方式来处理消息的消费逻辑
     *
     * @param method 消费者方法
     * @param handlerMap 所有的处理方式集合
     */
    private void startMessageListener(Method method, Map<RedisMQModeEnum, AbstractMessageHandler> handlerMap) {
        MessageListener listener = method.getAnnotation(MessageListener.class);
        if (null == listener) {
            return;
        }
        RedisMQModeEnum mode = listener.mode();
        AbstractMessageHandler handler = handlerMap.get(mode);
        if (handler == null) {
            return;
        }
        handler.invokeMessage(method);
    }

    /**
     * 获取处理程序
     * @return
     */
    private Map<RedisMQModeEnum, AbstractMessageHandler> getInvokers() {
        Map<String, Object> beansWithAnnotation = ApplicationContextUtils.applicationContext.getBeansWithAnnotation(MessageHandler.class);
        Map<RedisMQModeEnum, AbstractMessageHandler> collect =
                beansWithAnnotation.values().stream().collect(Collectors
                        .toMap(k -> k.getClass().getAnnotation(MessageHandler.class).value(), k -> (AbstractMessageHandler) k));
        return collect;
    }
}
