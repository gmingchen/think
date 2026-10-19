package com.dishes.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 商户付费意愿（docs/v13 §4，对应 {@code feedback_merchant.willingness} 列值）
 * <p>
 * 直接对应 v13 文档 §7 判定阈值：落在 {@code FREE} 的样本不计入「有效付费意愿」，
 * {@code LOW} 及以上才可能触发「扩一档会员」或「立项商户版」两条路径。
 * </p>
 *
 * @author 好市民
 */
@Getter
@AllArgsConstructor
public enum FeedbackWillingnessEnum implements EnumIntArray {
    /**
     * 付费意愿
     */
    FREE(1, "免费才用"),
    LOW(2, "20元以内每月"),
    MEDIUM(3, "20至50元每月"),
    HIGH(4, "50元以上或一次性买断"),
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
    public static final int[] ARRAY = Arrays.stream(values()).mapToInt(FeedbackWillingnessEnum::getCode).toArray();

    @Override
    public int[] array() {
        return ARRAY;
    }
}
