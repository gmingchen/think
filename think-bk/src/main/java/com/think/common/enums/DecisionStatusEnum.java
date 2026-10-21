package com.think.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 决策单状态（对应 decision.status）
 * 生命周期：草稿 → 评议中 → 已截止 → 已复盘 / 已关闭
 *
 * @author 好市民
 */
@Getter
@AllArgsConstructor
public enum DecisionStatusEnum implements EnumIntArray {
    /**
     * 状态
     */
    DRAFT(0, "草稿"),
    REVIEWING(1, "评议中"),
    ENDED(2, "已截止"),
    RECAPTURED(3, "已复盘"),
    CLOSED(4, "已关闭"),
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
    public static final int[] ARRAY = Arrays.stream(values()).mapToInt(DecisionStatusEnum::getCode).toArray();

    @Override
    public int[] array() {
        return ARRAY;
    }
}
