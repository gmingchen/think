package com.dishes.modules.member.model.vo;

import lombok.Data;
import lombok.experimental.Accessors;

import java.time.LocalDateTime;

/**
 * 创建订单结果VO（docs/v12 §7.2 第一段）
 * <p>
 * 2026-10-11 拆分接口后新增。它是「一条已落库的待支付订单」的回执，<b>不含任何签名</b>：
 * 签名要下一步拿 {@code /xpay/sign} 换。之所以把金额回给前端而不是让它自己算——
 * 首单资格与活动价都在服务端定（§7.2 要点 3），前端拿到什么就画什么，否则又多一处会算错的地方。
 * </p>
 * <p>
 * ⚠️ 这里的每一个值都来自 {@code member_order} 那一行快照而不是 {@code member_plan}：
 * 配置后来怎么改，本回执（以及后续签名）说的都是建单那一刻的价。
 * </p>
 * @author 好市民
 */
@Data
@Accessors(chain = true)
public class MemberOrderVO {
    /**
     * 业务单号，取签名与查单兜底都只认它
     */
    private String outTradeNo;
    /**
     * 套餐编码快照
     */
    private String planCode;
    /**
     * 展示名快照（「畅厨会员月卡」），前端确认弹窗用，不让它再去查一次价目表
     */
    private String title;
    /**
     * 档位快照：1-轻享 2-畅厨
     */
    private Integer tier;
    /**
     * 周期月数快照
     */
    private Integer months;
    /**
     * 道具原价（分），与 signData.goodsPrice 同值；前端画划线价用
     */
    private Integer goodsPrice;
    /**
     * 本单应实付金额（分）：首单为活动价，否则等于原价。发货校验的基准也是它（§7.4）
     */
    private Integer expectedPrice;
    /**
     * 应实付金额展示串（「¥2.9」），与 expectedPrice 同源，前端不硬编码价格
     */
    private String expectedPriceText;
    /**
     * 本单支付有效期（建单那一刻 + 15 分钟）。前端据它画倒计时，
     * 而不是自己算“我点了多久”——过期以服务端关单为准，两边各算一份就会不一致
     */
    private LocalDateTime expireAt;
    /**
     * 本单是否按畅厨首单价成立（建单那一刻的判定结果，前端只据它画提示语）
     */
    private Boolean firstOrder;
}
