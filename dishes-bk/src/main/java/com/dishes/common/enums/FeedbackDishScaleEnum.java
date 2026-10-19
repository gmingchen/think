package com.dishes.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 商户菜品规模（docs/v13 §4，对应 {@code feedback_merchant.dish_scale} 列值）
 * <p>
 * 用于对比现有家庭档上限（v11 定稿 {@code max_dish_count=50}），判断错配程度。
 * </p>
 *
 * @author 好市民
 */
@Getter
@AllArgsConstructor
public enum FeedbackDishScaleEnum implements EnumIntArray {
    /**
     * 菜品规模
     */
    TINY(1, "20以内"),
    SMALL(2, "20至50"),
    MEDIUM(3, "50至100"),
    LARGE(4, "100以上"),
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
    public static final int[] ARRAY = Arrays.stream(values()).mapToInt(FeedbackDishScaleEnum::getCode).toArray();

    @Override
    public int[] array() {
        return ARRAY;
    }
}
