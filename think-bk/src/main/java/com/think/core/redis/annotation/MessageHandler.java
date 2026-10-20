package com.think.core.redis.annotation;

import com.think.common.enums.RedisMQModeEnum;
import org.springframework.stereotype.Component;

import java.lang.annotation.*;

/**
 * 处理器注解，不同的类型使用不同的注解标准
 * @author 好市民
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface MessageHandler {

    RedisMQModeEnum value() default RedisMQModeEnum.TOPIC;

}
