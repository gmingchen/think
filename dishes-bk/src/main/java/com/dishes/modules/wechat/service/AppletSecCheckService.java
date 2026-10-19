package com.dishes.modules.wechat.service;

/**
 * 小程序内容安全审核
 * https://developers.weixin.qq.com/miniprogram/dev/OpenApiDoc/sec-center/sec-check/msgSecCheck.html
 * @author 好市民
 */
public interface AppletSecCheckService {

    /**
     * 文本内容安全审核：命中违规直接抛业务异常
     * <p>
     * 微信 UGC 场景的平台硬性要求；接口自身异常/限频时按放行处理（详见实现注释的权衡）
     * </p>
     * @param content 待审核文本（微信侧限制 2000 字以内，超出请调用方截断）
     * @param openid 内容发布者的 openid
     */
    void checkText(String content, String openid);
}
