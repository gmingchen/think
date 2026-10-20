package com.think.modules.wechat.service;

import com.think.modules.wechat.model.WechatResponse;
import com.think.modules.wechat.model.dto.AppletAccessTokenDTO;

import java.util.function.Function;

/**
 * 获取接口调用凭据
 * https://developers.weixin.qq.com/miniprogram/dev/OpenApiDoc/mp-access-token/getAccessToken.html
 * @author 好市民
 */
public interface WechatAccessTokenService {
    /**
     * 获取接口调用凭证
     * @return
     */
    AppletAccessTokenDTO getAccessToken();

    /**
     * 获取接口调用凭证 先从缓存中获取
     * @return
     */
    AppletAccessTokenDTO getAccessTokenCache();

    /**
     * 调用微信接口，凭证失效时清缓存重取凭证并重试一次
     * <p>
     * 同一 appid 的凭证全局唯一，别处（如本地环境）重新获取会让本处缓存的凭证失效，
     * 这类失效只有重取凭证才能恢复，靠用户重试同一份死凭证会一直失败
     * </p>
     * @param invoker 入参为 access_token，返回微信响应
     * @return 微信响应；重试后仍失败则原样返回，由调用方按业务判定
     */
    <T extends WechatResponse> T executeWithTokenRefresh(Function<String, T> invoker);
}
