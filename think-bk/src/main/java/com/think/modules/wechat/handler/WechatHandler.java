package com.think.modules.wechat.handler;

import cn.hutool.http.Header;
import cn.hutool.http.HttpRequest;
import cn.hutool.http.HttpUtil;
import cn.hutool.json.JSONUtil;
import com.alibaba.fastjson.JSONObject;
import com.think.common.enums.ResultCodeEnum;
import com.think.exception.RunException;
import com.think.modules.wechat.model.WechatResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;

import java.util.Map;

/**
 * 微信请求处理器
 * @author 好市民
 */
@Slf4j
public class WechatHandler {

    /**
     * 微信接口请求超时（毫秒）：hutool 默认不限时，接口卡住会把调用线程始终挂住；
     * msgSecCheck 已位于 AI 生图热路径上，必须有界超时（60s 足够覆盖正常响应，不致抢占业务超时预算）
     */
    private static final int REQUEST_TIMEOUT = 60000;

    /**
     * 微信小程序 GET 请求
     * @param url URL
     * @param params 参数
     * @param clazz 返回对象
     * @param <T> 泛型
     * @return
     */
    public static <T extends WechatResponse> T doGet(String url, Map<String, Object> params, Class<T> clazz) {
        String result = HttpUtil.get(url, params);
        return responseHandler(result, clazz);
    }
    /**
     * 微信小程序 POST 请求
     * @param url URL
     * @param params 参数
     * @param clazz 返回对象
     * @param <T> 泛型
     * @return
     */
    public static <T extends WechatResponse> T doPostJsonOnly(String url, Map<String, Object> params, Class<T> clazz) {
        String result = HttpRequest.post(url)
                .header(Header.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .body(JSONUtil.toJsonStr(params))
                .timeout(REQUEST_TIMEOUT)
                .execute()
                .body();
        log.debug("微信小程序-请求结果：{}", result);
        JSONObject jsonObject = JSONObject.parseObject(result);
        return jsonObject.toJavaObject(clazz);
    }

    /**
     * 微信小程序 POST 请求
     * @param url URL
     * @param params 参数
     * @param clazz 返回对象
     * @param <T> 泛型
     * @return
     */
    public static <T extends WechatResponse> T doPost(String url, Map<String, Object> params, Class<T> clazz) {
        String result = HttpUtil.post(url, params);
        return responseHandler(result, clazz);
    }

    /**
     * 微信小程序 POST 请求
     * @param url URL
     * @param params 参数
     * @return
     */
    public static byte[] doPost(String url, Map<String, Object> params) {
        return HttpRequest.post(url)
                .header(Header.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .body(JSONUtil.toJsonStr(params))
                .execute()
                .bodyBytes();
    }

    /**
     * 微信小程序 上传文件
     * @param url URL
     * @param params 参数
     * @param clazz 返回对象
     * @param <T> 泛型
     * @return
     */
    public static <T extends WechatResponse> T doUpload(String url, Map<String, Object> params, Class<T> clazz) {
        String result = HttpUtil.post(url, params);
        log.debug("微信小程序-请求结果：{}", result);
        JSONObject jsonObject = JSONObject.parseObject(result);
        return jsonObject.toJavaObject(clazz);
    }

    /**
     * 微信小程序 请求 API 响应处理
     * @param result 响应结果
     * @param clazz 返回对象
     * @param <T> 泛型
     * @return
     */
    public static <T extends WechatResponse> T responseHandler(String result, Class<T> clazz) {
        log.debug("微信小程序-请求结果：{}", result);
        JSONObject jsonObject = JSONObject.parseObject(result);
        T response = jsonObject.toJavaObject(clazz);
        if (response.getErrcode() != null && response.getErrcode() != 0) {
            log.debug("微信小程序-请求异常，code：{}，message：{}", response.getErrcode(), response.getErrmsg());
            throw new RunException(ResultCodeEnum.WECHAT_REQUEST_ERROR);
        }
        return response;
    }
}
