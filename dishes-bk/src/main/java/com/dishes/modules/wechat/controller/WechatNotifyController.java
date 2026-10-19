package com.dishes.modules.wechat.controller;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.dishes.modules.member.service.MemberService;
import com.dishes.modules.wechat.utils.WechatUtils;
import com.dishes.modules.wechat.xpay.config.XpayEnv;
import com.dishes.modules.wechat.xpay.config.XpayProperties;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 微信消息推送统一入口（v12 首次接上，此前 WechatUtils.checkSignature() 全项目零调用点）
 * <p>
 * ⚠️ 这个 URL 是一个小程序全局唯一的位置：配在这里之后，将来接客服消息、订阅消息回执等任何推送，
 * 都必须在同一个 controller 内按 MsgType / Event 分派，配不了第二个地址（docs/v12 §2.4）。
 * 所以它挂在 wechat 模块下而不是 member 模块——会员只是它要分派的事件之一。
 * </p>
 * <p>
 * 响应形状与项目其余接口不同：微信只认 {@code {"ErrCode":0,"ErrMsg":"..."}}，不认统一的 Result。
 * 因此本类内部自行兜住异常，绝不让异常飘到全局异常处理器去（那会把响应改写成 Result，微信读不到 ErrCode）。
 * </p>
 *
 * @author 好市民
 */
@Slf4j
@RestController
@RequestMapping("/wechat/notify")
public class WechatNotifyController {

    /**
     * 现金购买道具成功 → 主发放入口
     */
    private static final String EVENT_GOODS_DELIVER = "xpay_goods_deliver_notify";
    /**
     * 退款完成（含 Apple 侧裁处）→ 权益回收入口
     */
    private static final String EVENT_REFUND = "xpay_refund_notify";
    /**
     * Apple 退款问询：不回应答会被视为放弃申诉、直接退款，但不得据此提前回收权益
     */
    private static final String EVENT_IOS_REFUND_QUERY = "xpay_subscribe_ios_refund_query_notify";
    /**
     * 用户投诉：本版只留痕，人工在 MP 后台处理
     */
    private static final String EVENT_COMPLAINT = "xpay_complaint_notify";
    /**
     * 代币支付：本项目不用代币
     */
    private static final String EVENT_COIN_PAY = "xpay_coin_pay_notify";
    /**
     * 微信支付侧资金事件：不参与业务
     */
    private static final String EVENT_WXPAY_CALLBACK = "xpay_wxpay_callback_notify";

    /**
     * 处理成功（微信据此停止重推）
     */
    private static final int NOTIFY_OK = 0;
    /**
     * 处理失败，请微信按它的节奏重推（最多 15 次）
     */
    private static final int NOTIFY_RETRY = -1;

    @Autowired
    private XpayProperties xpayProperties;
    /**
     * 当前环境的微信 env（按 profile 现算）：用于拦下「沙箱推到现网、或反之」的错环境推送
     */
    @Autowired
    private XpayEnv xpayEnv;
    /**
     * 本文件对业务的唯一依赖点：会员是推送要分派的事件之一。将来接第二个虚拟产品时，
     * 是在下面那个 switch 里添一项（而不是把推送配置搬回某个业务模块）
     */
    @Autowired
    private MemberService memberService;

    /**
     * MP 后台保存「消息推送」配置时，微信服务器发来的 GET 校验
     *
     * @param signature 微信带来的签名
     * @param timestamp 时间戳
     * @param nonce     随机串
     * @param echostr   需原样回显的随机串
     * @return 验签通过时返回 echostr 原文，否则返回一段非 echostr 的短文本（后台表现为「Token校验失败」）
     */
    @GetMapping
    public String check(@RequestParam(required = false) String signature,
                        @RequestParam(required = false) String timestamp,
                        @RequestParam(required = false) String nonce,
                        @RequestParam(required = false) String echostr) {
        String token = xpayProperties.getNotifyToken();
        if (StringUtils.isBlank(token)) {
            // 微信侧只会回一句「Token校验失败」，看不出是配置缺失，故这里必须把配置项点名出来
            log.error("微信消息推送校验失败：xpay.notify-token 未配置，须与 MP 后台「开发设置 → 消息推送」的 Token 逐字一致");
            return "notify token not configured";
        }
        if (!WechatUtils.checkSignature(token, signature, timestamp, nonce)) {
            // 只落可比对的三项，Token 原文不进日志
            log.error("微信消息推送验签不通过，signature：{}，timestamp：{}，nonce：{}", signature, timestamp, nonce);
            return "signature mismatch";
        }
        if (StringUtils.isBlank(echostr)) {
            log.error("微信消息推送验签通过但 echostr 为空，请确认请求是否为完整的配置校验");
            return "echostr missing";
        }
        log.info("微信消息推送配置校验通过，已回显 echostr");
        return echostr;
    }

    /**
     * 事件推送接收，按 Event 分派
     *
     * @param signature 微信带来的签名
     * @param timestamp 时间戳
     * @param nonce     随机串
     * @param body      报文原文（后台数据格式为 JSON）
     * @return 固定为 {@code {"ErrCode":n,"ErrMsg":"..."}}
     */
    @PostMapping
    public Map<String, Object> receive(@RequestParam(required = false) String signature,
                                       @RequestParam(required = false) String timestamp,
                                       @RequestParam(required = false) String nonce,
                                       @RequestBody(required = false) String body) {
        try {
            // 明文模式下 body 不加密，signature 是唯一的来源证明。
            // ⚠️ 它只覆盖 token/timestamp/nonce、不覆盖 body：能挡掉不知道 Token 的伪造者，挡不住链路中间对 body 的篡改，
            // 所以发放前仍要查单复核（§7.4 口径已于 2026-10-11 定为「查单复核后再发放」）
            if (!WechatUtils.checkSignature(xpayProperties.getNotifyToken(), signature, timestamp, nonce)) {
                log.error("微信消息推送 POST 验签不通过，signature：{}，timestamp：{}，nonce：{}，body：{}",
                        signature, timestamp, nonce, body);
                return notifyResult(NOTIFY_RETRY, "signature mismatch");
            }
            if (StringUtils.isBlank(body)) {
                log.error("微信消息推送 POST 报文为空，signature：{}", signature);
                return notifyResult(NOTIFY_RETRY, "empty body");
            }
            String message = body.trim();
            if (message.startsWith("<")) {
                // 后台「数据格式」被改成 XML，或 xpay.msg-data-format 与它不一致；本版不实现 XML 解析
                log.error("收到 XML 报文但本端只实现 JSON 解析，请把 MP 后台「数据格式」保持为 JSON。body：{}", message);
                return notifyResult(NOTIFY_RETRY, "xml not supported");
            }

            JSONObject json = JSONUtil.parseObj(message);
            // 普通消息带 MsgType、事件带 Event，两类都可能推到这个地址
            String event = json.getStr("Event", json.getStr("MsgType", StringUtils.EMPTY));
            log.info("收到微信推送，event：{}，env：{}", event, json.getInt("Env"));

            switch (event) {
                case EVENT_GOODS_DELIVER:
                case EVENT_REFUND:
                    return handleMemberEvent(event, json, message);
                case EVENT_IOS_REFUND_QUERY:
                    // 必须应答（不回等于放弃申诉），但权益回收只以 xpay_refund_notify 为准，不在问询时扣天数
                    log.error("Apple 退款问询，留痕待人工跟进，本单不提前回收权益。body：{}", message);
                    return notifyResult(NOTIFY_OK, "success");
                case EVENT_COMPLAINT:
                case EVENT_COIN_PAY:
                case EVENT_WXPAY_CALLBACK:
                    log.error("非本版业务的微信推送事件，留痕待人工处理，event：{}，body：{}", event, message);
                    return notifyResult(NOTIFY_OK, "success");
                default:
                    // ⚠️ 未知事件必须回成功：回错误码会被重推 15 次，微信将来新增事件类型时会把日志淹掉
                    log.error("未识别的微信推送事件，event：{}，body：{}", event, message);
                    return notifyResult(NOTIFY_OK, "success");
            }
        } catch (Exception e) {
            log.error("微信消息推送处理异常，body：" + body, e);
            return notifyResult(NOTIFY_RETRY, "internal error");
        }
    }

    /**
     * 发放与回收两条主事件：转调 {@code MemberService.grant / revoke}
     * <p>
     * ⚠️ 通知在本链路里只当触发器，不是事实来源：grant 内部会先调 {@code /xpay/query_order} 确认这笔单真付了钱
     * 才写权益（§7.4，2026-10-11 定的口径）。所以本方法不判断金额、也不直接写库，只负责把字段递过去并翻译发放结果：
     * 已了结（发放成功 / 幂等重推 / 金额不符已关单）回 {@code ErrCode:0}，未确证回 {@code -1} 请微信重推。
     * </p>
     *
     * @param event   事件名
     * @param json    报文对象
     * @param message 报文原文（留痕用）
     * @return
     */
    private Map<String, Object> handleMemberEvent(String event, JSONObject json, String message) {
        Integer env = json.getInt("Env");
        if (env != null && env != xpayEnv.get()) {
            // 沙箱推到现网、或反之：本环境处理不了，重推也处理不了，故回成功丢弃并告警
            log.error("微信推送的 Env 与当前 profile 环境不一致，丢弃。推送 env：{}，本地 env：{}，body：{}",
                    env, xpayEnv.get(), message);
            return notifyResult(NOTIFY_OK, "env mismatch");
        }
        String outTradeNo = json.getStr("OutTradeNo");
        if (StringUtils.isBlank(outTradeNo)) {
            // 没有单号就定位不到订单，重推 15 次也不会突然多出单号来，回成功丢弃 + 告警留痕
            log.error("会员事件缺少 OutTradeNo，无法定位订单，丢弃。event：{}，body：{}", event, message);
            return notifyResult(NOTIFY_OK, "out trade no missing");
        }
        // 查单接口必填 openid，而 member_order 里没有这一列，所以只能从推送报文取（§7.4）
        String openid = json.getStr("OpenId");
        boolean settled;
        if (EVENT_GOODS_DELIVER.equals(event)) {
            // attach 里的 userId 不做交叉校验：伪造通知没有真实订单，查单必失败，这一步已由 grant 内部兜住
            settled = memberService.grant(outTradeNo, openid,
                    readActualPrice(json), readTransactionId(json), readMchOrderNo(json), readPaidTime(json));
        } else {
            settled = memberService.revoke(outTradeNo);
        }
        return settled ? notifyResult(NOTIFY_OK, "success") : notifyResult(NOTIFY_RETRY, "not settled yet");
    }

    /**
     * 取实付金额（分）
     * <p>
     * 官方推送把金额放在 {@code GoodsInfo.ActualPrice}，历史样本里也见过顶层 {@code TotalFee}，
     * 所以两处都试、取到为止。都取不到就递 null，由 grant 退回查单里的 {@code paid_fee} 作准。
     * </p>
     */
    private Integer readActualPrice(JSONObject json) {
        JSONObject goods = json.getJSONObject("GoodsInfo");
        if (goods != null && goods.getInt("ActualPrice") != null) {
            return goods.getInt("ActualPrice");
        }
        return json.getInt("TotalFee");
    }

    /**
     * 微信支付单号，落在 {@code WeChatPayInfo.TransactionId}
     */
    private String readTransactionId(JSONObject json) {
        JSONObject pay = json.getJSONObject("WeChatPayInfo");
        return pay != null ? pay.getStr("TransactionId") : json.getStr("TransactionId");
    }

    /**
     * 微信商户单号，落在 {@code WeChatPayInfo.MchOrderNo}
     */
    private String readMchOrderNo(JSONObject json) {
        JSONObject pay = json.getJSONObject("WeChatPayInfo");
        return pay != null ? pay.getStr("MchOrderNo") : json.getStr("MchOrderNo");
    }

    /**
     * 支付时间：{@code WeChatPayInfo.PaidTime} 是 unix 秒，按 +8 还原成 CST 本地时间与全库时间口径对齐
     */
    private LocalDateTime readPaidTime(JSONObject json) {
        JSONObject pay = json.getJSONObject("WeChatPayInfo");
        Long paid = pay != null ? pay.getLong("PaidTime") : json.getLong("PaidTime");
        if (paid == null || paid <= 0) {
            return null;
        }
        return LocalDateTime.ofEpochSecond(paid, 0, ZoneOffset.ofHours(8));
    }

    /**
     * 组装微信要求的响应
     *
     * @param errCode 0 表示处理完成，非 0 表示请重推
     * @param errMsg  说明文本，仅用于排查
     * @return
     */
    private Map<String, Object> notifyResult(int errCode, String errMsg) {
        // 用 Map 而不是 POJO：字段名是大写开头的 ErrCode / ErrMsg，走 POJO 会被 Jackson 按 getter 降成 errCode
        Map<String, Object> result = new LinkedHashMap<>(4);
        result.put("ErrCode", errCode);
        result.put("ErrMsg", errMsg);
        return result;
    }
}
