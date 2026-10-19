package com.dishes.modules.wechat.model;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 微信应用请求参数
 * @author 好市民
 */
@Accessors(chain = true)
@Data
public class WechatRequest {
    /**
     * 小程序唯一凭证，即 AppID
     */
    private String appid;
    /**
     * 小程序唯一凭证密钥，即 AppSecret
     */
    private String secret;
}
