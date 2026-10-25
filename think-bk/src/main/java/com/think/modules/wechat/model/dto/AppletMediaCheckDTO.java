package com.think.modules.wechat.model.dto;

import com.alibaba.fastjson.annotation.JSONField;
import com.think.modules.wechat.model.WechatResponse;
import lombok.Data;

/**
 * 多媒体内容安全识别（mediaCheckAsync）同步返回体
 * <p>
 * 该接口只立即返回 trace_id，真正的检测结论稍后通过「消息推送」异步下发
 * </p>
 * https://developers.weixin.qq.com/miniprogram/dev/OpenApiDoc/sec-center/sec-check/mediaCheckAsync.html
 *
 * @author 好市民
 */
@Data
public class AppletMediaCheckDTO extends WechatResponse {
    /**
     * 任务唯一标识，用于关联后续异步回调结果（微信字段为 snake_case，显式映射）
     */
    @JSONField(name = "trace_id")
    private String traceId;
}
