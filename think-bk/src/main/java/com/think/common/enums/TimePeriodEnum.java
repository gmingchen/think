package com.think.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 时段枚举
 * @author 好市民
 */
@Getter
@AllArgsConstructor
public enum TimePeriodEnum implements EnumIntArray {
    /**
     * 时段
     */
    BREAKFAST(1, "早餐"),
    LUNCH(2, "午餐"),
    DINNER(3, "晚餐"),
    MIDNIGHT_SNACK(4, "宵夜"),
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
    public static final int[] ARRAY = Arrays.stream(values()).mapToInt(TimePeriodEnum::getCode).toArray();

    @Override
    public int[] array() {
        return ARRAY;
    }
}
