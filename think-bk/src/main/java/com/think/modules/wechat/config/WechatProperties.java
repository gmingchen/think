package com.think.modules.wechat.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 微信小程序配置属性
 *
 * @author 好市民
 */
@Data
@Component
@ConfigurationProperties(prefix = "wechat")
public class WechatProperties {

    /**
     * 小程序appId
     */
    private String appId;

    /**
     * 小程序appSecret
     */
    private String appSecret;

    /**
     * 消息推送 Token，须与 MP 后台「开发设置 → 消息推送」的 Token 逐字一致。
     * 属于小程序全局唯一的推送入口 {@code /wechat/notify}，与具体业务无关。
     */
    private String notifyToken;

    /**
     * 消息推送的数据格式：json / xml，与后台「数据格式」单选项同值；本版只实现 JSON 解析
     */
    private String msgDataFormat = "json";
}
