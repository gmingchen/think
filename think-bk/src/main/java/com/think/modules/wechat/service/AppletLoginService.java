package com.think.modules.wechat.service;

import com.think.modules.wechat.model.dto.AppletLoginDTO;

/**
 * 小程序登录
 * https://developers.weixin.qq.com/miniprogram/dev/OpenApiDoc/user-login/code2Session.html
 * @author 好市民
 */
public interface AppletLoginService {

    /**
     * 小程序登录
     * @param code 登录时获取的 code，可通过wx.login获取
     * @return
     */
    AppletLoginDTO login(String code);
}
