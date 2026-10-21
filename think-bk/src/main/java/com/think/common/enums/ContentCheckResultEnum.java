package com.think.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 单次内容审核结果（对应 decision_check.check_status / review_check.check_status 流水）
 * 注意：与闸门态 {@link CheckStatusEnum} 语义不同，这里是「每一次送审」的结果留痕
 *
 * @author 好市民
 */
@Getter
@AllArgsConstructor
public enum ContentCheckResultEnum implements EnumIntArray {
    /**
     * 单次审核结果
     */
    PENDING(0, "待审"),
    PASS(1, "通过"),
    REJECT(2, "不通过"),
    MANUAL(3, "需人工复审"),
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
    public static final int[] ARRAY = Arrays.stream(values()).mapToInt(ContentCheckResultEnum::getCode).toArray();

    @Override
    public int[] array() {
        return ARRAY;
    }
}
