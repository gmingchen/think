package com.think.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 举报处理状态（对应 abuse_report.handle_status）
 * MVP 无运营后台：仅落库留痕，处置延后
 *
 * @author 好市民
 */
@Getter
@AllArgsConstructor
public enum AbuseHandleStatusEnum implements EnumIntArray {
    /**
     * 处理状态
     */
    PENDING(0, "待处理"),
    HANDLED(1, "已处理(下架)"),
    REJECTED(2, "驳回"),
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
    public static final int[] ARRAY = Arrays.stream(values()).mapToInt(AbuseHandleStatusEnum::getCode).toArray();

    @Override
    public int[] array() {
        return ARRAY;
    }
}
