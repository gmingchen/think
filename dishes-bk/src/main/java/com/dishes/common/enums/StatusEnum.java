package com.dishes.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 通用状态枚举
 * @author 好市民
 */
@Getter
@AllArgsConstructor
public enum StatusEnum implements EnumIntArray {
    /**
     * 状态
     */
    DISABLE(0, "禁用"),
    ENABLE(1, "启用"),
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
    public static final int[] ARRAY = Arrays.stream(values()).mapToInt(StatusEnum::getCode).toArray();

    @Override
    public int[] array() {
        return ARRAY;
    }
}
