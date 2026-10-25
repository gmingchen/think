package com.think.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 多媒体送审资产类型（media_check.biz_type）
 * <p>
 * 仅作流水记录的业务分类，不作为 DTO 校验字段，故不实现 EnumIntArray
 * </p>
 *
 * @author 好市民
 */
@Getter
@AllArgsConstructor
public enum MediaBizTypeEnum {
    /**
     * 决策单图片：bizId=决策单ID，target=图片下标
     */
    DECISION_IMAGE(1, "决策单图片"),
    /**
     * 用户头像：bizId=用户ID，target 为空
     */
    USER_AVATAR(2, "用户头像"),
    ;

    /**
     * 类型值
     */
    private final Integer code;
    /**
     * 类型名
     */
    private final String message;

    public static MediaBizTypeEnum of(Integer code) {
        for (MediaBizTypeEnum value : values()) {
            if (value.code.equals(code)) {
                return value;
            }
        }
        return null;
    }
}
