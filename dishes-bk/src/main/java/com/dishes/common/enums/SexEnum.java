package com.dishes.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 性别枚举
 * @author 好市民
 */
@Getter
@AllArgsConstructor
public enum SexEnum implements EnumIntArray {
    /**
     * 性别
     */
    FEMALE(0, "女"),
    MALE(1, "男"),
    UNKNOWN(2, "未知"),
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
    public static final int[] ARRAY = Arrays.stream(values()).mapToInt(SexEnum::getCode).toArray();

    @Override
    public int[] array() {
        return ARRAY;
    }
}
