package com.think.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 群组状态（对应 grp.status）
 *
 * @author 好市民
 */
@Getter
@AllArgsConstructor
public enum GrpStatusEnum implements EnumIntArray {
    /**
     * 状态
     */
    NORMAL(1, "正常"),
    DISSOLVED(2, "已解散"),
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
    public static final int[] ARRAY = Arrays.stream(values()).mapToInt(GrpStatusEnum::getCode).toArray();

    @Override
    public int[] array() {
        return ARRAY;
    }
}
