package com.dishes.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.Arrays;

/**
 * 商户现用管理方式（docs/v13 §4，对应 {@code feedback_merchant.current_tool} 列值，多选）
 * <p>
 * 多选字段以逗号分隔存储本枚举的 code（不做中间关联表，v13 文档 §6.2 设计要点 1），
 * 校验发生在 {@code FeedbackMerchantServiceImpl} 内：{@code @Enum} 注解只能校验单个 Integer，
 * 不支持多选列表（{@code List<Integer>}），故不能用注解直接挡。
 * </p>
 *
 * @author 好市民
 */
@Getter
@AllArgsConstructor
public enum FeedbackCurrentToolEnum implements EnumIntArray {
    /**
     * 管理方式
     */
    WECHAT_GROUP(1, "微信群口头沟通"),
    HANDWRITTEN(2, "手写小票或白板"),
    EXCEL(3, "Excel或备忘录"),
    DELIVERY_PLATFORM(4, "外卖平台后台"),
    POS_SAAS(5, "专业收银SaaS"),
    NONE(6, "没有固定方式"),
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
    public static final int[] ARRAY = Arrays.stream(values()).mapToInt(FeedbackCurrentToolEnum::getCode).toArray();

    @Override
    public int[] array() {
        return ARRAY;
    }
}
