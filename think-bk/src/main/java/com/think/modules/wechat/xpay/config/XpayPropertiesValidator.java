package com.think.modules.wechat.xpay.config;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;

/**
 * 虚拟支付通道配置启动体检（docs/v12 §9.1 / §12.1）
 * <p>
 * 本类只管**配置自己**：凭证与推送参数齐不齐。env 已改为按 profile 现算（见 {@link XpayEnv}），不再是配置项，本处也无从对齐校验。
 * 「配置与表数据是否一致」（{@code member_plan} 里基线六个 code 在不在、价格填得对不对）归业务侧的
 * {@code MemberPlanValidator}，因为只有它要查表。两侧都以「宁起不来也不带错配置起来」为准，
 * 但硬拦与只告警的分界见下面每条的说明。
 * </p>
 *
 * @author 好市民
 */
@Slf4j
@Component
public class XpayPropertiesValidator {

    /**
     * 本版只实现 JSON 报文解析（后台已定 JSON + 明文模式），xml 分支没有样本、写了也验不了（§16 第 12 条②）
     */
    private static final String FORMAT_JSON = "json";

    @Autowired
    private XpayProperties xpayProperties;

    @Autowired
    private XpayEnv xpayEnv;

    @PostConstruct
    public void validate() {
        if (StringUtils.isBlank(xpayProperties.getNotifyToken())) {
            // 缺它 JAR 能起来、业务也能跑，但后台按「消息推送-提交」必然失败，且微信只报一句「Token校验失败」
            log.error("xpay 配置告警：xpay.notify-token 未配置，MP 后台「开发设置 → 消息推送」的 GET 验签会失败，" +
                    "发货通知也进不来（POST 侧验签同样用它）");
        }
        if (!FORMAT_JSON.equalsIgnoreCase(StringUtils.trimToEmpty(xpayProperties.getMsgDataFormat()))) {
            log.error("xpay 配置告警：xpay.msg-data-format 当前为 {}，本端只实现 JSON 解析；" +
                    "若 MP 后台「数据格式」选的是 XML，收到的报文会被丢弃并回 ErrCode:-1", xpayProperties.getMsgDataFormat());
        }
        if (StringUtils.isBlank(xpayProperties.getAppKey()) || StringUtils.isBlank(xpayProperties.getOfferId())) {
            // 只告警不阻启动：置灰版（member.enabled=false）本来就可能带着未申请的凭证先发版；
            // 真正的闸门在下单与查单两处（XpaySignService.assertReady / XpayClient.queryOrder）
            log.error("xpay 配置告警：xpay.app-key 或 xpay.offer-id 未配置。放开付费前必须补齐**与当前 profile 环境匹配**的值，"
                    + "（现网 profile 用现网凭证、其余用沙箱凭证）否则 prepay 会全部拒单、发货通知也无法复核（权益永不落库）");
        }
        log.info("xpay 通道配置校验通过，env={}，msgDataFormat={}，notifyToken 已配置={}，凭证已配置={}",
                xpayEnv.get(), xpayProperties.getMsgDataFormat(),
                StringUtils.isNotBlank(xpayProperties.getNotifyToken()),
                StringUtils.isNotBlank(xpayProperties.getAppKey()) && StringUtils.isNotBlank(xpayProperties.getOfferId()));
    }
}
