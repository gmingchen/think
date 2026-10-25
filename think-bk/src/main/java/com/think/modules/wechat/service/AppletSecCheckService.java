package com.think.modules.wechat.service;

/**
 * 小程序内容安全审核
 * https://developers.weixin.qq.com/miniprogram/dev/OpenApiDoc/sec-center/sec-check/msgSecCheck.html
 * @author 好市民
 */
public interface AppletSecCheckService {

    /**
     * 场景值 1-资料：昵称/群名/个性签名等短文本
     */
    int SCENE_PROFILE = 1;
    /**
     * 场景值 2-评论：评议观点等
     */
    int SCENE_COMMENT = 2;
    /**
     * 场景值 3-论坛：公开/群内决策单等可被多人浏览的内容
     */
    int SCENE_FORUM = 3;
    /**
     * 场景值 4-社交日志：复盘「实际结局」等一次性记录
     */
    int SCENE_SOCIAL = 4;

    /**
     * 文本内容安全审核：命中违规（risky/review）或接口自身异常时按 fail-close 直接抛业务异常
     * <p>
     * 微信 UGC 场景的平台硬性要求；调用方结构校验已放宽到不做语义判断，本服务是全链路唯一内容防线
     * </p>
     * @param content 待审核文本（微信侧限制 2000 字以内，超出本方法内部截断）
     * @param openid 内容发布者的 openid（须近两小时内访问过小程序）
     * @param scene 场景值，取本接口定义的 SCENE_* 常量
     */
    void checkText(String content, String openid, int scene);
}
