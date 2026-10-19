package com.dishes.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 商户经营类型（docs/v13 §4，对应 {@code feedback_merchant.business_type} 列值）
 * <p>
 * 本版为需求验证收集用，前端有一份同值的 BUSINESS_TYPE_LIST 常量镜像，两侧改一处必须同步改另一处。
 * </p>
 *
 * @author 好市民
 */
@Getter
@AllArgsConstructor
public enum FeedbackBusinessTypeEnum implements EnumIntArray {
    /**
     * 经营类型
     */
    HOME_RESTAURANT(1, "私房菜"),
    SMALL_SHOP(2, "小餐饮门店"),
    STREET_STALL(3, "摆摊"),
    BNB_GROUP_MEAL(4, "民宿或团体餐"),
    CANTEEN(5, "员工食堂"),
    OTHER(6, "其他"),
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
    public static final int[] ARRAY = Arrays.stream(values()).mapToInt(FeedbackBusinessTypeEnum::getCode).toArray();

    @Override
    public int[] array() {
        return ARRAY;
    }
}
