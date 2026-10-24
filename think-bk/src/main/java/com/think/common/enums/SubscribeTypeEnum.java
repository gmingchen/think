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
     * 评议时限到期提醒（§3.3）：决策单评议时限截止时提醒发起人查看报告
     */
    COOL_DOWN_END("COOL_DOWN_END", "评议时限到期提醒"),
    // 预留其类通知待后续需求接入时再定义，避免留下没有实现方的枚举值
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
