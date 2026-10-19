package com.dishes.modules.member.model.vo;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 「我的」页会员卡文案（四态全由后端下发，docs/v12 §4.1）
 * <p>
 * 前稿的 mineEntryLabel 单个字符串已废弃：一行标签装不下「主标题 + 副权益 + 价格 + 按钮 + 色调」五件事，
 * 改成结构体后前端只做渲染。价格串走这里而不是让前端拼，是为了「置灰期→放开期只改配置不发版」。
 * </p>
 * @author 好市民
 */
@Data
@Accessors(chain = true)
public class MemberMineCardVO {
    /**
     * 卡片主标题
     */
    private String title;
    /**
     * 副文案（权益摘要或到期说明）
     */
    private String desc;
    /**
     * 价格串，如「¥1.9 起」；生效中/已过期态为 null（那两态不催价）
     */
    private String price;
    /**
     * 右侧动作按钮文案
     */
    private String action;
    /**
     * 色调：plain-未开通 | gold-生效中（含临期） | expired-已过期 | disabled-功能未开放。
     * 前端只按值套样式，不拼文案
     */
    private String tone;
}
