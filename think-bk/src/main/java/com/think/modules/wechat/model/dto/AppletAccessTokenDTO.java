package com.think.modules.wechat.model.dto;

import com.think.modules.wechat.model.WechatResponse;
import lombok.Data;

/**
 * 小程序接口调用凭证响应参数
 * @author 好市民
 */
@Data
public class AppletAccessTokenDTO extends WechatResponse {
    /**
     * 获取到的凭证
     */
    private String access_token;
    /**
     * 凭证有效时间，单位：秒。目前是7200秒之内的值。
     */
    private Integer expires_in;
}
