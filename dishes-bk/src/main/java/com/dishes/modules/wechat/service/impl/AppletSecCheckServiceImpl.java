package com.dishes.modules.wechat.service.impl;

import com.dishes.common.utils.StringUtils;
import com.dishes.exception.RunException;
import com.dishes.modules.wechat.handler.WechatHandler;
import com.dishes.modules.wechat.model.dto.AppletSecCheckDTO;
import com.dishes.modules.wechat.service.AppletSecCheckService;
import com.dishes.modules.wechat.service.WechatAccessTokenService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * 小程序文本内容安全审核
 * <p>
 * 本服务是全链路唯一的内容防线（调用方的结构校验已放宽到不判语义），因此接口异常时 fail-close
 * </p>
 * @author 好市民
 */
@Slf4j
@Service("appletSecCheckService")
public class AppletSecCheckServiceImpl implements AppletSecCheckService {

    /**
     * 接口版本：v2 才返回 result/label 明细
     */
    private static final int VERSION = 2;
    /**
     * 场景值 1-资料（昵称/简介等短文本），本项目用于菜名与菜谱描述
     */
    private static final int SCENE_PROFILE = 1;
    /**
     * 审核结论：通过
     */
    private static final String RESULT_PASS = "pass";
    /**
     * 微信侧 content 长度上限，超出直接截断（送审目的是拦违规词，截断不影响判断）
     */
    private static final int MAX_CONTENT_LEN = 2000;

    @Autowired
    private WechatAccessTokenService wechatAccessTokenService;

    @Override
    public void checkText(String content, String openid) {
        if (content == null || content.trim().isEmpty()) {
            return;
        }
        Map<String, Object> params = new HashMap<>(4);
        params.put("content", content.length() > MAX_CONTENT_LEN ? content.substring(0, MAX_CONTENT_LEN) : content);
        params.put("version", VERSION);
        params.put("scene", SCENE_PROFILE);
        params.put("openid", openid);

        AppletSecCheckDTO response;
        try {
            // 凭证失效（被别处重新获取顶替）在网关层自愈重试一次，否则用户重试永远撞同一份死凭证
            response = wechatAccessTokenService.executeWithTokenRefresh(accessToken -> WechatHandler.doPostJsonOnly(
                    "https://api.weixin.qq.com/wxa/msg_sec_check?access_token=" + accessToken,
                    params, AppletSecCheckDTO.class));
        } catch (Exception e) {
            // fail-close：结构层已放宽到不做语义判断，本审核是唯一内容防线，接口异常/限频时宁可让用户重试也不能裸奔
            log.error("内容安全审核调用异常，本次拒绝：{}", e.getMessage());
            throw new RunException("内容审核服务繁忙，请稍后再试");
        }
        if (response == null || (response.getErrcode() != null && response.getErrcode() != 0)) {
            // prod 环境 com.dishes 日志级别为 ERROR，warn/info 不会落盘，拦截原因必须用 error 记录
            log.error("内容安全审核返回异常，本次拒绝：{}", response == null ? "null" : response.getErrmsg());
            throw new RunException("内容审核服务繁忙，请稍后再试");
        }
        AppletSecCheckDTO.Result result = response.getResult();
        if (result == null || StringUtils.isBlank(result.getSuggest())) {
            // errcode=0 却读不到结论：微信侧改了响应结构，归入「服务不可用」而不是伪装成内容违规
            log.error("内容安全审核响应缺少 result.suggest，本次拒绝：errcode={}, errmsg={}",
                    response.getErrcode(), response.getErrmsg());
            throw new RunException("内容审核服务繁忙，请稍后再试");
        }
        if (RESULT_PASS.equalsIgnoreCase(result.getSuggest())) {
            return;
        }
        // review（疑似）一并拒绝：AI 生图产出的是可对外分享的图片，风险容忍度须低于普通文本
        log.error("内容安全审核未通过，suggest={}, label={}, detail={}, openid={}, content={}",
                result.getSuggest(), result.getLabel(), response.getDetail(), openid, content);
        throw new RunException("内容涉嫌违规，已拒绝生成，请修改后重试");
    }
}
