package com.think.modules.wechat.xpay.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 微信虚拟支付通道配置（docs/v12 §2.3 / §7.4）
 * <p>
 * 本类只装「怎么收钱」的事实：凭证、消息推送。它与「卖什么」无关，所以会员与将来任何虚拟商品
 * 共用这一份配置；某个商品是否在卖，由业务模块自己的开关决定（会员是 {@code member.enabled}），
 * 通道本身不设开关——关掉它等于关掉全站收款。
 * </p>
 * <p>
 * 启动体检不在本类，在同包的 {@link XpayPropertiesValidator}：{@code @ConfigurationProperties} 类里塞依赖
 * 会把「配置绑定」与「数据体检」两件事绑死。SKU 与价格也不在这里（在 {@code member_plan} 表，§8.3），
 * 否则会回到「表与 yml 两处都能生效」的双头状态。
 * </p>
 *
 * @author 好市民
 */
@Data
@Component
@ConfigurationProperties(prefix = "xpay")
public class XpayProperties {

    /**
     * 虚拟支付 OfferID（支付应用ID），取自 MP 后台「虚拟支付 → 基本配置」
     */
    private String offerId;

    /**
     * 虚拟支付 AppKey，必须与当前环境匹配：现网填现网 Key、沙箱填沙箱 Key，两者不通用
     * <p>
     * 现网/沙箱由 profile 决定（见 {@link XpayEnv}），本类不再持有 env 配置项——避免「yml 的 env」与「profile」两处真相写反。
     * ⚠️ 现网 profile 下本项是**发货的硬依赖**：C 端 paySig 与 B 端 query_order 都用它签。
     * 发放口径已定为「查单复核后再发放」（§7.4），缺它不是降级而是发不了货，
     * 所以 {@code XpaySignService.assertReady()} 会在下单前直接拒单（不拿空 Key 去签，
     * 那只会换来一个不易认的 -15006，把排查方向带到签名算法上）。
     * </p>
     */
    private String appKey;

    /**
     * 消息推送 Token，须与 MP 后台「开发设置 → 消息推送」的 Token 逐字一致。
     * 两边不一致时 GET 验签算出的 SHA1 对不上，而后台只会报一句「Token校验失败，请检查确认」，
     * 看不出是配置问题——故 {@code WechatNotifyController} 在取不到值时会点名本配置项。
     * <p>
     * 本项本来就属于**小程序全局唯一的推送入口**而不是某个业务，这也是它从 member 配置里搬出来的原因之一。
     * </p>
     */
    private String notifyToken;

    /**
     * 消息推送的数据格式：json / xml，与后台「数据格式」单选项同值。
     * 本版只实现 JSON 解析（后台已定 JSON + 明文模式），配成 xml 会解析不出事件并落 error 日志
     */
    private String msgDataFormat = "json";
}
