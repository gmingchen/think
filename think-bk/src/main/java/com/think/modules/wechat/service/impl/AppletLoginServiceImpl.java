package com.think.modules.wechat.service.impl;

import com.think.modules.wechat.config.WechatProperties;
import com.think.modules.wechat.handler.WechatHandler;
import com.think.modules.wechat.model.dto.AppletLoginDTO;
import com.think.modules.wechat.service.AppletLoginService;
import org.springframework.stereotype.Service;

import org.springframework.beans.factory.annotation.Autowired;
import java.util.HashMap;
import java.util.Map;

/**
 * @author 好市民
 */
@Service("appletLoginService")
public class AppletLoginServiceImpl implements AppletLoginService {

    @Autowired
    private WechatProperties wechatProperties;

    @Override
    public AppletLoginDTO login(String code) {
        String url = "https://api.weixin.qq.com/sns/jscode2session";

        Map<String, Object> params = new HashMap<>(4);
        params.put("appid", wechatProperties.getAppId());
        params.put("secret", wechatProperties.getAppSecret());
        params.put("js_code", code);
        params.put("grant_type", "client_credential");

        return WechatHandler.doGet(url, params, AppletLoginDTO.class);
    }
}
