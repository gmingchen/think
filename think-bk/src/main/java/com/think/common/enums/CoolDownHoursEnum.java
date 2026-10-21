package com.think.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 冷静期时长档位（对应 decision.cool_down_hours）
 * <p>
 * MVP 提供 12 小时 / 24 小时 / 3 天 / 7 天 四档（§4.1）；具体数值留待实现阶段按需微调，不影响数据结构。
 * </p>
 *
 * @author 好市民
 */
@Getter
@AllArgsConstructor
public enum CoolDownHoursEnum implements EnumIntArray {
    /**
     * 冷静期档位（小时）
     */
    H12(12, "12 小时"),
    H24(24, "24 小时"),
    H72(72, "3 天"),
    H168(168, "7 天"),
    ;

    /**
     * 类型值（小时）
     */
    private final Integer code;
    /**
     * 类型名
     */
    private final String message;
    /**
     * 枚举值数组
     */
    public static final int[] ARRAY = Arrays.stream(values()).mapToInt(CoolDownHoursEnum::getCode).toArray();

    @Override
    public int[] array() {
        return ARRAY;
    }
}
