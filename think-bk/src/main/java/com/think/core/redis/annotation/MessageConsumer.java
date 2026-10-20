package com.think.core.redis.annotation;

import org.springframework.core.annotation.AliasFor;
import org.springframework.stereotype.Component;

import java.lang.annotation.*;

/**
 * 消费者注解
 * @author 好市民
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Component
public @interface MessageConsumer {

    @AliasFor(annotation = Component.class)
    String value() default "";

}