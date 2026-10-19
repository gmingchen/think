package com.dishes.modules.wechat.service.impl;

import com.dishes.modules.wechat.handler.WechatHandler;
import com.dishes.modules.wechat.model.WechatResponse;
import com.dishes.modules.wechat.service.AppletSubscribeService;
import com.dishes.modules.wechat.service.WechatAccessTokenService;
import org.springframework.stereotype.Service;

import org.springframework.beans.factory.annotation.Autowired;
import java.util.HashMap;
import java.util.Map;

/**
 * @author 好市民
 */
@Service("appletSubscribeService")
public class AppletSubscribeServiceImpl implements AppletSubscribeService {

    @Autowired
    private WechatAccessTokenService wechatAccessTokenService;

    @Override
    public WechatResponse push(String templateId, String openId, Map<String, Object> data, String page) {
        Map<String, Object> params = new HashMap<>(3);
        params.put("template_id", templateId);
        params.put("touser", openId);
        params.put("data", data);
        params.put("page", page);

        // 凭证失效（被别处重新获取顶替）自愈重试一次
        return wechatAccessTokenService.executeWithTokenRefresh(accessToken -> WechatHandler.doPostJsonOnly(
                "https://api.weixin.qq.com/cgi-bin/message/subscribe/send?access_token=" + accessToken,
                params, WechatResponse.class));
    }
}
