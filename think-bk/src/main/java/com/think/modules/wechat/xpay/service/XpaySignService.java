package com.think.modules.wechat.xpay.service;

import cn.hutool.core.util.RandomUtil;
import cn.hutool.json.JSONUtil;
import com.think.exception.RunException;
import com.think.modules.wechat.xpay.config.XpayEnv;
import com.think.modules.wechat.xpay.config.XpayProperties;
import com.think.modules.wechat.xpay.model.vo.XpayPrepayVO;
import com.think.modules.wechat.xpay.util.XpaySignUtils;
import lombok.Data;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 虚拟支付下单侧：signData 组装 + 双签名（docs/v12 §2.3 / §7.2）
 * <p>
 * 本类是「整条链路上最容易写错的那一步」的唯一归宿：{@code signData} 在这里序列化成字符串之后，
 * 就再没有任何人重新序列化它——签名算的是它，发给微信的也是它（同一个串）。这条铁律一旦破，
 * 表现就是微信侧 -15005/-15006/-15007，而日志里看不出任何异常（§7.2 要点 3 / §17 第 6 条）。
 * </p>
 * <p>
 * 之所以从会员模块抽出来放在通道包下：装字段、序列化、两把 HMAC 这套动作，
 * 与「卖的是会员还是别的道具」完全无关；<b>哪个字段该填什么值</b>（原价还是活动价、attach 里塞谁）
 * 才是业务判断，那部分留在调用方。
 * </p>
 *
 * @author 好市民
 */
@Slf4j
@Component
public class XpaySignService {

    /**
     * C 端下单的签名 uri，官方固定串，不带斜杠与参数
     */
    private static final String URI_REQUEST_VIRTUAL_PAYMENT = "requestVirtualPayment";
    /**
     * 道具直购模式，官方固定值
     */
    private static final String MODE_GOODS = "short_series_goods";
    /**
     * 币种，官方要求 CNY
     */
    private static final String CURRENCY_CNY = "CNY";
    private static final DateTimeFormatter NO_FORMATTER = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");
    /**
     * 随机后缀字符池：只用小写字母与数字，避开官方单号约束里那些要额外说明的符号
     */
    private static final String RANDOM_POOL = "abcdefghijklmnopqrstuvwxyz0123456789";
    /**
     * 随机后缀长度：14 位时间 + 6 位随机 + 前缀，落在官方 8~32 位区间内且长度固定，便于对账时按位截
     */
    private static final int RANDOM_LEN = 6;

    @Autowired
    private XpayProperties xpayProperties;

    @Autowired
    private XpayEnv xpayEnv;

    /**
     * 生成业务单号：{@code bizPrefix + yyyyMMddHHmmss + 6 位随机}
     * <p>
     * 前缀由业务自带（会员用 mv = member virtual），这样商户后台与对账脚本一眼能分出钱来自哪条产品线；
     * 生成规则收在通道侧，是为了让「长度合法、不以 _ 开头、字符池安全」这三条只写一遍（§7.2 要点 1）。
     * </p>
     *
     * @param bizPrefix 业务前缀，2~4 个字符为宜，须以字母开头
     * @return 单号原文
     */
    public String nextOutTradeNo(String bizPrefix) {
        String prefix = StringUtils.defaultIfBlank(bizPrefix, "xp");
        return prefix + LocalDateTime.now().format(NO_FORMATTER) + RandomUtil.randomString(RANDOM_POOL, RANDOM_LEN);
    }

    /**
     * 凭证闸门：AppKey / OfferID 缺任一项就直说，不拿空串去签名。
     * <p>
     * ⚠️ 拿空 Key 签名不会报「配置缺失」，而是报 -15006「签名不一致」——排查方向会被带到算法上，
     * 而真正的问题只是 yml 里那一行没填。业务侧应当在<b>落订单之前</b>先调本方法，
     * 否则会留下一条永远不会支付的单。
     * </p>
     */
    public void assertReady() {
        if (StringUtils.isBlank(xpayProperties.getAppKey()) || StringUtils.isBlank(xpayProperties.getOfferId())) {
            log.error("虚拟支付不可用：xpay.app-key 或 xpay.offer-id 未配置，env：{}（§2.2 第 2 步，现网 Key 与沙箱 Key 不通用）",
                    xpayEnv.get());
            throw new RunException("支付配置不完整，请稍后再试");
        }
    }

    /**
     * 组装道具直购的下单参数与双签名
     *
     * @param goods      道具与价格（业务侧算好再传进来）
     * @param sessionKey 登录时拿到的会话密钥，来自服务端快照而不是前端
     * @return 前端原样透传给 wx.requestVirtualPayment 的出参
     */
    public XpayPrepayVO buildGoodsPayment(Goods goods, String sessionKey) {
        assertReady();
        if (goods == null || StringUtils.isBlank(goods.getProductId())
                || StringUtils.isBlank(goods.getOutTradeNo())
                || goods.getGoodsPrice() == null || goods.getGoodsPrice() <= 0) {
            // 这三样任缺其一，微信侧回的都是不易定位的错误码（-15013 一类），所以在此点名并拒单
            log.error("虚拟支付下单参数非法，productId：{}，outTradeNo：{}，goodsPrice：{}",
                    goods == null ? null : goods.getProductId(),
                    goods == null ? null : goods.getOutTradeNo(),
                    goods == null ? null : goods.getGoodsPrice());
            throw new RunException("商品信息不完整，请稍后再试");
        }
        if (StringUtils.isBlank(sessionKey)) {
            // 业务侧本该在落单前就查过一次（免得留下死单），这里是防「调用顺序写错」的兜底；
            // 文案必须保持「登录态已过期」，前端据此触发重新登录后重试（§2.3）
            throw new RunException("登录态已过期，请重试");
        }

        Map<String, Object> signDataMap = new LinkedHashMap<>();
        signDataMap.put("offerId", xpayProperties.getOfferId());
        signDataMap.put("buyQuantity", goods.getBuyQuantity() == null ? 1 : goods.getBuyQuantity());
        signDataMap.put("env", xpayEnv.get());
        signDataMap.put("currencyType", CURRENCY_CNY);
        signDataMap.put("productId", goods.getProductId());
        // goodsPrice 必须是道具原价，填活动价微信侧直接报 -15013（§7.2 要点 3）
        signDataMap.put("goodsPrice", goods.getGoodsPrice());
        if (goods.getActivityPrice() != null && goods.getActivityPrice() > 0) {
            // 不享活动价时这个字段整个不下发，而不是传 0
            signDataMap.put("activitySellingPrice", goods.getActivityPrice());
        }
        signDataMap.put("outTradeNo", goods.getOutTradeNo());
        // attach 原样透传（会员放的是 userId），发货通知里可交叉校验，防单号伪造（§7.2 要点 5）
        signDataMap.put("attach", StringUtils.defaultString(goods.getAttach()));
        // 先序列化成串，再用同一个串既签名又下发——下游（含前端）一次都不许重新序列化
        String signData = JSONUtil.toJsonStr(signDataMap);

        return new XpayPrepayVO()
                .setOutTradeNo(goods.getOutTradeNo())
                .setSignData(signData)
                .setPaySig(XpaySignUtils.buildPaySig(URI_REQUEST_VIRTUAL_PAYMENT, signData, xpayProperties.getAppKey()))
                .setSignature(XpaySignUtils.buildSignature(signData, sessionKey))
                .setMode(MODE_GOODS);
    }

    /**
     * 道具直购的入参：只描述「卖哪个道具、多少钱」，不含任何业务表字段
     */
    @Data
    @Accessors(chain = true)
    public static class Goods {
        /**
         * 微信道具 ID，由业务从自己的 SKU 表（会员是 member_plan.product_id）取好再传进来。
         * ⚠️ 本行前稿写的是「沙箱与现网不同，由业务按 env 取」，已于 2026-10-11 作废：
         * 后台「线上版本」只是开发版本发布后的只读镜像，道具只有一套 ID（docs/v12 §8.3）。
         * 留着那句比删掉更坏——它会让后来人以为这里该按环境分支，而那正是当日删掉的东西
         */
        private String productId;
        /**
         * 道具原价（分）。必须等于 MP 后台配置的道具价，否则 -15013
         */
        private Integer goodsPrice;
        /**
         * 活动价 / 首单价（分），为空或小于等于 0 表示本单不享活动价
         */
        private Integer activityPrice;
        /**
         * 业务单号，由 {@link #nextOutTradeNo(String)} 生成，且必须与业务订单表里落的那一条一致
         */
        private String outTradeNo;
        /**
         * 透传字段，发货通知里原样带回（会员业务用它带 userId）
         */
        private String attach;
        /**
         * 购买数量，道具直购场景恒为 1
         */
        private Integer buyQuantity;
    }
}
