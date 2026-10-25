package com.think.modules.wechat.controller;

import cn.hutool.json.JSONObject;
import cn.hutool.json.JSONUtil;
import com.think.modules.wechat.config.WechatProperties;
import com.think.modules.wechat.service.AppletMediaCheckService;
import com.think.modules.wechat.utils.WechatUtils;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 微信消息推送统一入口
 * <p>
 * ⚠️ 这个 URL 是一个小程序全局唯一的位置：配在这里之后，将来接客服消息、订阅消息回执等任何推送，
 * 都必须在同一个 controller 内按 MsgType / Event 分派，配不了第二个地址。
 * </p>
 * <p>
 * 响应形状与项目其余接口不同：微信只认 {@code {"ErrCode":0,"ErrMsg":"..."}}，不认统一的 Result。
 * 因此本类内部自行兜住异常，绝不让异常飘到全局异常处理器去（那会把响应改写成 Result，微信读不到 ErrCode）。
 * </p>
 * <p>
 * 本版仅承担 GET 验签 + POST 留痕，任何具体业务分派在下面的 receive 里按事件名扩展。
 * </p>
 *
 * @author 好市民
 */
@Slf4j
@RestController
@RequestMapping("/wechat/notify")
public class WechatNotifyController {

    /**
     * 处理成功（微信据此停止重推）
     */
    private static final int NOTIFY_OK = 0;
    /**
     * 处理失败，请微信按它的节奏重推（最多 15 次）
     */
    private static final int NOTIFY_RETRY = -1;

    @Autowired
    private WechatProperties wechatProperties;
    @Autowired
    private AppletMediaCheckService appletMediaCheckService;

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
        String token = wechatProperties.getNotifyToken();
        if (StringUtils.isBlank(token)) {
            // 微信侧只会回一句「Token校验失败」，看不出是配置缺失，故这里必须把配置项点名出来
            log.error("微信消息推送校验失败：wechat.notify-token 未配置，须与 MP 后台「开发设置 → 消息推送」的 Token 逐字一致");
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
     * 事件推送接收：本版仅验签 + 留痕，返回 OK 停推。将来接新事件在 switch 里加分支即可
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
            // 明文模式下 body 不加密，signature 是唯一的来源证明：能挡掉不知道 Token 的伪造者，挡不住链路中间对 body 的篡改
            if (!WechatUtils.checkSignature(wechatProperties.getNotifyToken(), signature, timestamp, nonce)) {
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
                // 后台「数据格式」被改成 XML，或 wechat.msg-data-format 与它不一致；本版不实现 XML 解析
                log.error("收到 XML 报文但本端只实现 JSON 解析，请把 MP 后台「数据格式」保持为 JSON。body：{}", message);
                return notifyResult(NOTIFY_RETRY, "xml not supported");
            }

            JSONObject json = JSONUtil.parseObj(message);
            // 普通消息带 MsgType、事件带 Event，两类都可能推到这个地址
            String event = json.getStr("Event", json.getStr("MsgType", StringUtils.EMPTY));
            log.info("收到微信推送，event：{}，env：{}，body：{}", event, json.getInt("Env"), message);

            // 多媒体内容安全异步结果（wxa_media_check）：命中该事件则内部消化并回 OK 停推
            if (appletMediaCheckService.handleMediaCheck(json)) {
                return notifyResult(NOTIFY_OK, "success");
            }

            // TODO: 其余事件按 event 分派到具体业务；未接的事件一律留痕后回 OK
            return notifyResult(NOTIFY_OK, "success");
        } catch (Exception e) {
            log.error("微信消息推送处理异常，body：" + body, e);
            return notifyResult(NOTIFY_RETRY, "internal error");
        }
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
