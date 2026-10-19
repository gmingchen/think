package com.dishes.modules.wechat.xpay.client;

import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpResponse;
import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.dishes.modules.wechat.service.WechatAccessTokenService;
import com.dishes.modules.wechat.xpay.config.XpayEnv;
import com.dishes.modules.wechat.xpay.config.XpayProperties;
import com.dishes.modules.wechat.xpay.util.XpaySignUtils;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 微信虚拟支付服务端客户端（docs/v12 §9.1）
 * <p>
 * 形态对齐既有 LlmClient / ImageClient：hutool HTTP + 有界超时，不引微信 SDK（§9.4）。
 * 本类是通道能力，不认识任何业务表：它只回答「微信那边这笔单到底是什么状态」，
 * 「确证之后给谁发什么权益」由调用方（当前是 {@code MemberServiceImpl}）决定。
 * </p>
 * <p>
 * ⚠️ 本类是钱的路径上唯一的「事实来源」：发放口径已定为查单复核（§7.4，2026-10-11 拍板），
 * 所以 {@link #queryOrder} 拿不到确证时必须返回 null 让调用方**不发**，
 * 而不是抛异常让调用方"当作成功"——宁可让用户等一下重试，也不能凭一条可被篡改的通知就发货。
 * </p>
 *
 * @author 好市民
 */
@Slf4j
@Component
public class XpayClient {

    private static final String HOST = "https://api.weixin.qq.com";
    /**
     * 查单接口路径。参与 paySig 的就是这个串，不带问号与参数
     */
    private static final String URI_QUERY_ORDER = "/xpay/query_order";

    /**
     * 有界超时（毫秒）。查单发生在微信发货通知的处理线程里，拖太久会被判定为超时并重推，
     * 5s 足够覆盖正常响应，又不至于让一次外网抖动把通知线程挂死
     */
    private static final int REQUEST_TIMEOUT = 5000;

    /**
     * 订单已支付、可发放的三个状态：2-已支付待发货 3-发货中 4-已发货。
     * 0/1 还没付，5/7/8 涉及退款，6 已关闭，都不该发权益
     */
    private static final int STATUS_PAID_TO_DELIVER = 2;
    private static final int STATUS_DELIVERING = 3;
    private static final int STATUS_DELIVERED = 4;

    @Autowired
    private XpayProperties xpayProperties;
    @Autowired
    private XpayEnv xpayEnv;
    @Autowired
    private WechatAccessTokenService wechatAccessTokenService;

    /**
     * 查现金单（官方接口 query_order）
     *
     * @param openid     下单用户的 openid（本接口必填，业务订单表未存，由调用方从推送报文或登录快照取）
     * @param outTradeNo 业务单号
     * @return 查得订单信息；任何一环拿不到确证都返回 null（调用方据此不发放）
     */
    public XpayOrder queryOrder(String openid, String outTradeNo) {
        String appKey = xpayProperties.getAppKey();
        if (StringUtils.isBlank(appKey)) {
            // 查单复核依赖现网 AppKey，缺了就发不了货，这一条必须点名配置项
            log.error("虚拟支付查单跳过：xpay.app-key 未配置，无法复核订单。outTradeNo：{}", outTradeNo);
            return null;
        }
        if (StringUtils.isBlank(openid) || StringUtils.isBlank(outTradeNo)) {
            log.error("虚拟支付查单参数缺失，openid：{}，outTradeNo：{}", openid, outTradeNo);
            return null;
        }

        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("openid", openid);
        payload.put("env", xpayEnv.get());
        payload.put("order_id", outTradeNo);
        // 先序列化成串、再用同一个串既签名又发送。签一次、发送时让框架再序列化一次是本接口最常见的错法
        String body = JSONUtil.toJsonStr(payload);
        String paySig = XpaySignUtils.buildPaySig(URI_QUERY_ORDER, body, appKey);

        try {
            // 官方文档的 query 参数只有 access_token 与 pay_sig；两者都是 URL 安全字符，直接拼不必转义
            String url = HOST + URI_QUERY_ORDER + "?access_token="
                    + wechatAccessTokenService.getAccessTokenCache().getAccess_token() + "&pay_sig=" + paySig;
            String response;
            try (HttpResponse executed = HttpRequest.post(url)
                    .header("Content-Type", "application/json")
                    .body(body)
                    .timeout(REQUEST_TIMEOUT)
                    .execute()) {
                response = executed.body();
            }
            return parseResponse(response, outTradeNo);
        } catch (Exception e) {
            log.error("虚拟支付查单异常，outTradeNo：" + outTradeNo, e);
            return null;
        }
    }

    /**
     * 解析查单响应：errcode 非 0 或缺 order 节点都按「拿不到确证」处理
     */
    private XpayOrder parseResponse(String response, String outTradeNo) {
        if (StringUtils.isBlank(response)) {
            log.error("虚拟支付查单返回空响应，outTradeNo：{}", outTradeNo);
            return null;
        }
        JSONObject json;
        try {
            json = JSONUtil.parseObj(response);
        } catch (Exception e) {
            log.error("虚拟支付查单响应不是 JSON，outTradeNo：{}，response：{}", outTradeNo, StringUtils.abbreviate(response, 500));
            return null;
        }
        Integer errcode = json.getInt("errcode", 0);
        if (errcode != 0) {
            // 268490003 是签名错误、268490001 是 openid 错误，都在这条日志里现形
            log.error("虚拟支付查单失败，outTradeNo：{}，errcode：{}，errmsg：{}", outTradeNo, errcode, json.getStr("errmsg"));
            return null;
        }
        JSONObject order = json.getJSONObject("order");
        if (order == null) {
            log.error("虚拟支付查单无 order 节点，outTradeNo：{}，response：{}", outTradeNo, StringUtils.abbreviate(response, 500));
            return null;
        }
        XpayOrder result = new XpayOrder();
        result.setStatus(order.getInt("status"));
        result.setOrderFee(order.getInt("order_fee", 0));
        result.setPaidFee(order.getInt("paid_fee", 0));
        result.setOrderId(order.getStr("order_id"));
        result.setWxOrderId(order.getStr("wx_order_id"));
        result.setWxpayOrderId(order.getStr("wxpay_order_id"));
        result.setPaidTime(order.getLong("paid_time", 0L));
        result.setEnvType(order.getInt("env_type", 0));
        return result;
    }

    /**
     * 订单是否处于「已付款且未退款」状态（可发放）
     */
    public static boolean isPaid(XpayOrder order) {
        if (order == null || order.getStatus() == null) {
            return false;
        }
        int status = order.getStatus();
        return status == STATUS_PAID_TO_DELIVER || status == STATUS_DELIVERING || status == STATUS_DELIVERED;
    }

    /**
     * query_order 返回体的 order 节点（只取本系统要用的字段，其余留原始日志）
     */
    @Data
    public static class XpayOrder {
        /**
         * 当前状态，枚举见官方 Res.order.status
         */
        private Integer status;
        /**
         * 订单金额（分）
         */
        private Integer orderFee;
        /**
         * 用户实付金额（分）。首单活动价时它小于 orderFee，所以校验要拿它比业务侧的应付金额
         */
        private Integer paidFee;
        /**
         * 业务单号
         */
        private String orderId;
        /**
         * 微信内部单号
         */
        private String wxOrderId;
        /**
         * 微信支付交易单号，由调用方落到自己订单表的 transaction_id
         */
        private String wxpayOrderId;
        /**
         * 支付时间（unix 秒级）
         */
        private Long paidTime;
        /**
         * 环境类型：1-现网 2-沙箱。注意它与请求体 env 的 0/1 不是同一套编码
         */
        private Integer envType;
    }
}
