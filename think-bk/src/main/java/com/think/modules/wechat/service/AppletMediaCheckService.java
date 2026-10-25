package com.think.modules.wechat.service;

import cn.hutool.json.JSONObject;
import com.think.common.enums.MediaBizTypeEnum;

/**
 * 小程序多媒体内容安全审核（mediaCheckAsync，异步）
 * https://developers.weixin.qq.com/miniprogram/dev/OpenApiDoc/sec-center/sec-check/mediaCheckAsync.html
 *
 * @author 好市民
 */
public interface AppletMediaCheckService {

    /**
     * 提交一张图片异步送审：调用 mediaCheckAsync 拿 trace_id 并落一条待审流水。
     * <p>
     * 「先放行、回调命中再处理」：本方法为 fire-and-forget，任何异常只记日志、绝不影响调用方的主业务
     * </p>
     * @param bizType  资产类型（决策单图片 / 用户头像）
     * @param bizId    业务主键（决策单ID 或 用户ID）
     * @param target   图片下标（头像传 null）
     * @param mediaUrl 图片URL（须为公网可访问地址，微信服务器会来拉取）
     * @param openid   内容发布者 openid
     * @param scene    场景值，取 {@link AppletSecCheckService} 定义的 SCENE_* 常量
     */
    void submitImageCheck(MediaBizTypeEnum bizType, Long bizId, Integer target,
                          String mediaUrl, String openid, int scene);

    /**
     * 处理「消息推送」里的 wxa_media_check 事件：按 trace_id 反查流水，命中违规(risky/review)则摘图/重置头像。
     * @param json 已解析的推送报文
     * @return 是否为本次处理的事件（true=已消费并应回 OK 停推；false=非媒体审核事件，交回上层留痕）
     */
    boolean handleMediaCheck(JSONObject json);
}
