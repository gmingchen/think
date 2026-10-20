package com.think.modules.wechat.service;

import com.think.modules.wechat.model.WechatResponse;

import java.util.Map;

/**
 * 发送订阅消息
 * https://developers.weixin.qq.com/miniprogram/dev/OpenApiDoc/mp-message-management/subscribe-message/sendMessage.html
 * @author 好市民
 */
public interface AppletSubscribeService {

    /**
     * 发送订阅消息
     * @param templateId 模版ID
     * @param openId 用户OpenId
     * @param data 数据
     * @param page 页面
     * @return
     */
    WechatResponse push(String templateId, String openId, Map<String, Object> data, String page);

}
