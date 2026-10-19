package com.dishes.modules.member.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 会员业务配置（v12）
 * <p>
 * 本类只剩**会员这一个商品的销售开关**。虚拟支付的凭证与环境（offerId / appKey / env / notifyToken /
 * msgDataFormat）已于 2026-10-11 拆到通道层 {@code com.dishes.modules.wechat.xpay.config.XpayProperties}：
 * 它们描述的是「怎么收钱」，与卖什么无关，将来任何虚拟商品都共用那一份；把推送 Token 放在会员配置里，
 * 会让全局推送入口 {@code WechatNotifyController} 反过来依赖会员模块（它本来就还要分派客服消息等别的事件）。
 * </p>
 * <p>
 * 通道层因此<b>没有</b>总开关——关掉它等于关掉全站收款；要不要卖会员是业务判断，收在这里。
 * SKU 与价格也不在本类（在 {@code member_plan} 表，§8.3），否则会回到「表与 yml 两处都能生效」的双头状态。
 * 表数据体检在同包 {@link MemberPlanValidator}，通道配置体检在 {@code XpayPropertiesValidator}。
 * </p>
 *
 * @author 好市民
 */
@Data
@Component
@ConfigurationProperties(prefix = "member")
public class MemberProperties {

    /**
     * 会员购买总开关：false 时前端入口置灰、{@code order/create} 与 {@code paysign} 服务端都拒单（置灰版先发版，道具与 product_id 就绪后再放开）。
     * <p>
     * ⚠️ 它不影响<b>已有权益</b>的判定：免广告、无限容量、会员卡「已于 X 到期」都照旧生效，
     * 否则一次回滚就把付过钱的人的履约记录抹掉了（§4.1 态序口径）。
     * </p>
     */
    private Boolean enabled = false;
}
