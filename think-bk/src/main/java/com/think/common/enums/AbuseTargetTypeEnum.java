package com.think.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 举报对象类型（对应 abuse_report.target_type）
 *
 * @author 好市民
 */
@Getter
@AllArgsConstructor
public enum AbuseTargetTypeEnum implements EnumIntArray {
    /**
     * 举报对象类型
     */
    DECISION(1, "决策单"),
    REVIEW(2, "评议"),
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
    public static final int[] ARRAY = Arrays.stream(values()).mapToInt(AbuseTargetTypeEnum::getCode).toArray();

    @Override
    public int[] array() {
        return ARRAY;
    }
}
