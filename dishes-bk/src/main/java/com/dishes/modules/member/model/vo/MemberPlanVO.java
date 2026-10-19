package com.dishes.modules.member.model.vo;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 套餐VO（由 member_plan 当前行映射，docs/v12 §7.1）
 * <p>
 * ⚠️ 不含 productId：它属敏感配置，且 signData 由后端组装，前端拿不到也不需要。
 * 价格与省额都由后端下发（金额单位是分），前端不硬编码，避免与道具价漂移引发投诉。
 * </p>
 * @author 好市民
 */
@Data
@Accessors(chain = true)
public class MemberPlanVO {
    /**
     * 套餐编码，下单时原样回传
     */
    private String code;
    /**
     * 档位：1-轻享 2-畅厨
     */
    private Integer tier;
    /**
     * 周期月数（1/3/12，6 为半年付预留），前端胶囊文案由它推导
     */
    private Integer months;
    /**
     * 展示名（兼道具名）
     */
    private String name;
    /**
     * 权益摘要，按档位给（表里没有 desc 列，权益文案属产品口径，见 §8.3 末）
     */
    private String desc;
    /**
     * 售价（分）
     */
    private Integer price;
    /**
     * 售价展示串，如 ¥7.9
     */
    private String priceText;
    /**
     * 首单价（分），仅 pro_month 有值；为 null 表示该 SKU 无首单钩子
     */
    private Integer firstPrice;
    /**
     * 首单价展示串，如 ¥2.9
     */
    private String firstPriceText;
    /**
     * 相对月付省多少（分），0 表示不显示省额
     */
    private Integer saved;
}
