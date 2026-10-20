package com.think.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * redis 消息队列
 * @author 好市民
 */
@Getter
@AllArgsConstructor
public enum RedisMQModeEnum implements EnumIntArray {
    /**
     * redis 消息队列
     */
    /**
     * topic 模式，主题订阅
     */
    TOPIC(1, "主题订阅"),
    /**
     * pub/sub 模式 订阅发布 广播
     */
    PUBSUB(2, "广播"),
    /**
     * stream 模式
     */
    STREAM(3, "stream"),
    ;

    /**
     * 类型值
     */
    private final Integer code;
    /**
     * 类型名
     */
    private final String message;
    /**
     * 枚举值数组
     */
    public static final int[] ARRAY = Arrays.stream(values()).mapToInt(RedisMQModeEnum::getCode).toArray();

    @Override
    public int[] array() {
        return ARRAY;
    }
}
