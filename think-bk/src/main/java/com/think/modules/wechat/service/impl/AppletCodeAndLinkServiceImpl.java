package com.think.modules.wechat.service.impl;

import com.think.common.utils.ApplicationContextUtils;
import com.think.modules.wechat.handler.WechatHandler;
import com.think.modules.wechat.service.AppletCodeAndLinkService;
import com.think.modules.wechat.service.WechatAccessTokenService;
import org.springframework.stereotype.Service;

import org.springframework.beans.factory.annotation.Autowired;
import java.util.HashMap;
import java.util.Map;

/**
 * 小程序码与小程序链接
 * @author 好市民
 */
@Service("appletCodeAndLinkService")
public class AppletCodeAndLinkServiceImpl implements AppletCodeAndLinkService {

    @Autowired
    private WechatAccessTokenService wechatAccessTokenService;

    @Override
    public byte[] getUnlimitedQrcodeByte(String scene, String page) {
        String accessToken = wechatAccessTokenService.getAccessTokenCache().getAccess_token();
        String url = "https://api.weixin.qq.com/wxa/getwxacodeunlimit?access_token=" + accessToken;

        Map<String, Object> params = new HashMap<>(3);
        params.put("scene", scene);
        params.put("page", page);
        params.put("is_hyaline", false);
        params.put("env_version", ApplicationContextUtils.isProduction() ? "release" : "develop");
        params.put("check_path", false);

        return WechatHandler.doPost(url, params);
    }
}
