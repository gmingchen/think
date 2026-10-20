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
}
