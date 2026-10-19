package com.dishes.modules.wechat.service.impl;

import com.dishes.common.constant.RedisConstant;
import com.dishes.core.redis.utils.RedisUtils;
import com.dishes.modules.wechat.config.WechatProperties;
import com.dishes.modules.wechat.handler.WechatHandler;
import com.dishes.modules.wechat.model.WechatResponse;
import com.dishes.modules.wechat.model.dto.AppletAccessTokenDTO;
import com.dishes.modules.wechat.service.WechatAccessTokenService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import org.springframework.beans.factory.annotation.Autowired;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * 获取接口调用凭据
 * @author 好市民
 */
@Slf4j
@Service("wechatAccessTokenService")
public class WechatAccessTokenServiceImpl implements WechatAccessTokenService {

    /**
     * 凭证无效或不是最新：本处缓存的凭证已被别处重新获取顶替
     */
    private static final int ERRCODE_INVALID_CREDENTIAL = 40001;
    /**
     * 凭证超时：本处缓存的凭证已过期
     */
    private static final int ERRCODE_ACCESS_TOKEN_EXPIRED = 42001;

    @Autowired
    private RedisUtils redisUtils;

    @Autowired
    private WechatProperties wechatProperties;

    @Override
    public AppletAccessTokenDTO getAccessToken() {
        String url = "https://api.weixin.qq.com/cgi-bin/token";

        Map<String, Object> params = new HashMap<>(3);
        params.put("appid", wechatProperties.getAppId());
        params.put("secret", wechatProperties.getAppSecret());
        params.put("grant_type", "client_credential");

        return WechatHandler.doGet(url, params, AppletAccessTokenDTO.class);
    }

    @Override
    public AppletAccessTokenDTO getAccessTokenCache() {
        String key = RedisConstant.WECHAT_ACCESS_TOKEN;
        AppletAccessTokenDTO accessToken = redisUtils.get(key, AppletAccessTokenDTO.class).orElse(null);

        if (accessToken == null) {
            accessToken = this.getAccessToken();
            long seconds = 60 * 60 * 2L - 60 * 2L;
            redisUtils.set(key, accessToken, seconds);
        }

        return accessToken;
    }

    @Override
    public <T extends WechatResponse> T executeWithTokenRefresh(Function<String, T> invoker) {
        T response = invoker.apply(getAccessTokenCache().getAccess_token());
        if (!isTokenInvalid(response)) {
            return response;
        }
        // prod 环境 com.dishes 日志级别为 ERROR，失效原因必须用 error 才落盘
        log.error("微信凭证已失效，清缓存重取后重试一次：errcode={}, errmsg={}",
                response.getErrcode(), response.getErrmsg());
        redisUtils.delete(RedisConstant.WECHAT_ACCESS_TOKEN);
        // 只重试一次：重取后仍失败则原样返回，交给调用方按业务判定，避免反复打凭证接口
        return invoker.apply(getAccessTokenCache().getAccess_token());
    }

    /**
     * 判定响应是否为「凭证不可用」，这类错误换个凭证就能过，不属于业务失败
     */
    private boolean isTokenInvalid(WechatResponse response) {
        if (response == null || response.getErrcode() == null) {
            return false;
        }
        int errcode = response.getErrcode();
        return errcode == ERRCODE_INVALID_CREDENTIAL || errcode == ERRCODE_ACCESS_TOKEN_EXPIRED;
    }
}
