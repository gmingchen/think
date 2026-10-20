package com.think.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 订阅消息通知类型（对应 {@code subscribe_template.type} 列值，也是 {@code subscribe_log.type}）
 * <p>
 * 类型是「业务语义」的稳定标识，微信模板 ID 只是它的一个属性（存在 subscribe_template 表里，可换）。
 * 发送方按 type 找模板、扣配额，从而不把微信那串 ID 硬编码进业务代码（v9 注意事项 3）。
 * </p>
 *
 * @author 好市民
 */
@Getter
@AllArgsConstructor
public enum SubscribeTypeEnum {
    /**
     * 会员到期提醒（v12）：到期前 3 天推送续费提醒
     */
    MEMBER_EXPIRE("MEMBER_EXPIRE", "会员到期提醒"),
    // 以下三类是 v9 设计给 cook-order 预留（接单/上菜/取消通知），本版未接入发送、模板 ID 也仍空，
    // 待 v9 那套下厨订单通知落地时再放开，故此处先不定义，避免留下没有实现方的枚举值
    ;

    /**
     * 类型值（存库的字符串）
     */
    private final String code;
    /**
     * 类型名
     */
    private final String message;
}
