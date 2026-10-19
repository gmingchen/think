package com.dishes.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 厨房角色枚举
 * @author 好市民
 */
@Getter
@AllArgsConstructor
public enum KitchenRoleEnum implements EnumIntArray {
    /**
     * 角色
     */
    OWNER(1, "厨师长"),
    MANAGER(2, "管厨"),
    GUEST(3, "食客"),
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
    public static final int[] ARRAY = Arrays.stream(values()).mapToInt(KitchenRoleEnum::getCode).toArray();

    @Override
    public int[] array() {
        return ARRAY;
    }
}
