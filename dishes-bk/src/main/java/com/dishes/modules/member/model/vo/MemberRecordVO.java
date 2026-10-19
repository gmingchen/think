package com.dishes.modules.member.model.vo;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 开通记录行（会员中心页「开通记录」，取本人最近 5 条）
 * @author 好市民
 */
@Data
@Accessors(chain = true)
public class MemberRecordVO {
    /**
     * 微信支付交易单号：与用户微信账单里的「交易单号」一致，前端点记录行复制、客服按它查单退款；
     * 商户单号 out_trade_no 不外透（只存在于商家系统，用户在账单里对不上）
     */
    private String transactionId;
    /**
     * 套餐编码（前端按它取档位名与周期文案）
     */
    private String planCode;
    /**
     * 成交金额（分）：等于该单应实付，首单为活动价
     */
    private Integer amount;
    /**
     * 支付时间
     */
    private LocalDateTime paidAt;
    /**
     * 状态：1-已发放 2-已退款
     */
    private Integer status;
}
