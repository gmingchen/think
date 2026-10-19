package com.dishes.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 烹饪订单状态枚举
 * @author 好市民
 */
@Getter
@AllArgsConstructor
public enum CookOrderStatusEnum implements EnumIntArray {
    /**
     * 状态
     */
    PLACED(1, "已下单"),
    ACCEPTED(2, "已接单"),
    SERVED(3, "已上菜"),
    REVIEWED(4, "已评价"),
    CANCELLED(5, "已取消"),
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
    public static final int[] ARRAY = Arrays.stream(values()).mapToInt(CookOrderStatusEnum::getCode).toArray();

    @Override
    public int[] array() {
        return ARRAY;
    }
}
