package com.think.modules.wechat.model.dto;

import com.think.modules.wechat.model.WechatResponse;
import lombok.Data;

/**
 * 小程序登录响应参数
 * @author 好市民
 */
@Data
public class AppletLoginDTO extends WechatResponse {
    /**
     * 会话密钥
     */
    private String session_key;
    /**
     * 用户唯一标识
     */
    private String openid;
    /**
     * 用户在开放平台的唯一标识符，若当前小程序已绑定到微信开放平台账号下会返回
     */
    private String unionid;
}
