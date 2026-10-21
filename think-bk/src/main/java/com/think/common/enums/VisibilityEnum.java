package com.think.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 决策单可见范围（对应 decision.visibility）
 *
 * @author 好市民
 */
@Getter
@AllArgsConstructor
public enum VisibilityEnum implements EnumIntArray {
    /**
     * 可见范围
     */
    SELF(1, "仅自己(草稿)"),
    GRP(2, "群内"),
    PUBLIC(3, "公开"),
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
    public static final int[] ARRAY = Arrays.stream(values()).mapToInt(VisibilityEnum::getCode).toArray();

    @Override
    public int[] array() {
        return ARRAY;
    }
}
