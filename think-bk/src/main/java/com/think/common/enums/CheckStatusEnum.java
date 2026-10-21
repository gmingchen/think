package com.think.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 审核闸门状态（对应 decision.check_status / review.check_status）
 * 仅公开档使用：公开档须 =PASS(2) 才由草稿进入评议中
 *
 * @author 好市民
 */
@Getter
@AllArgsConstructor
public enum CheckStatusEnum implements EnumIntArray {
    /**
     * 闸门状态
     */
    NONE(0, "无需审核"),
    PENDING(1, "待审核"),
    PASS(2, "通过"),
    REJECT(3, "不通过"),
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
    public static final int[] ARRAY = Arrays.stream(values()).mapToInt(CheckStatusEnum::getCode).toArray();

    @Override
    public int[] array() {
        return ARRAY;
    }
}
