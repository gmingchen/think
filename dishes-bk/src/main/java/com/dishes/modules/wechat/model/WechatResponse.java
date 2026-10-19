package com.dishes.modules.wechat.model;

import lombok.Data;

/**
 * 微信响应参数
 * @author 好市民
 */
@Data
public class WechatResponse {
    /**
     * 错误码
     */
    private Integer errcode;
    /**
     * 错误信息
     */
    private String errmsg;
}
