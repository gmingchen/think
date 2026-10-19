package com.dishes.modules.wechat.xpay.model.vo;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 虚拟支付下单出参（前端只透传给 wx.requestVirtualPayment，一个字都不加工，docs/v12 §7.2）
 * <p>
 * 这五个字段全是通道产物、没有一个是业务字段，所以它不属于某个商品模块：将来接第二个虚拟产品时，
 * 它的 {@code /xxx/prepay} 回的还是这同一个结构。
 * </p>
 * <p>
 * ⚠️ {@code signData} 是后端已经序列化好的字符串原文，签名就是对它算的。
 * 前端若 JSON.stringify 一次（键顺序或空格变化）或后端再序列化一次，微信侧就报签名不一致，
 * 所以本字段类型是 String 而不是对象。
 * </p>
 *
 * @author 好市民
 */
@Data
@Accessors(chain = true)
public class XpayPrepayVO {
    /**
     * 业务单号，前端拿它调各自的 confirm 接口轮询（会员是 /member/confirm）
     */
    private String outTradeNo;
    /**
     * 支付参数 JSON 原文（含 offerId/buyQuantity/env/currencyType/productId/goodsPrice/outTradeNo/attach，
     * 享活动价时多一个 activitySellingPrice）
     */
    private String signData;
    /**
     * 支付签名 = hex(hmac_sha256(appKey, "requestVirtualPayment&" + signData))
     */
    private String paySig;
    /**
     * 用户态签名 = hex(hmac_sha256(sessionKey, signData))
     */
    private String signature;
    /**
     * 支付模式，道具直购固定 short_series_goods
     */
    private String mode;
}
