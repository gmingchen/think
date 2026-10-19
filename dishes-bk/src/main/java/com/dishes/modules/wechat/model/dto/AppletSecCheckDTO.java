package com.dishes.modules.wechat.model.dto;

import com.dishes.modules.wechat.model.WechatResponse;
import lombok.Data;

import java.util.List;

/**
 * 文本内容安全审核响应参数
 * https://developers.weixin.qq.com/miniprogram/dev/OpenApiDoc/sec-center/sec-check/msgSecCheck.html
 * @author 好市民
 */
@Data
public class AppletSecCheckDTO extends WechatResponse {
    /**
     * v2 的综合结论嵌在 result 对象内（{suggest, label}），顶层并不存在 suggest/label 字段；
     * 按 v1 的平铺结构声明会让结论永远读不到，任何文本都会被误判为「内容涉嫌违规」
     */
    private Result result;
    /**
     * 各识别策略的明细（keyword / content_model ...），仅用于日志定位命中的是哪一路
     */
    private List<Detail> detail;

    /**
     * 审核结论
     */
    @Data
    public static class Result {
        /**
         * 审核建议：pass / review / risky
         */
        private String suggest;
        /**
         * 命中的风险类型值（100 正常 / 200001 广告 / 200002 时政 / 210001 色情 ...）
         */
        private Integer label;
    }

    /**
     * 单路识别策略的结果；strategy=keyword 时微信不返 suggest/label，因此字段全部可空
     */
    @Data
    public static class Detail {
        /**
         * 识别策略：keyword（关键词）/ content_model（内容模型）等
         */
        private String strategy;
        /**
         * 该策略调用错误码
         */
        private Integer errcode;
        /**
         * 该策略给出的建议
         */
        private String suggest;
        /**
         * 该策略命中的风险类型值
         */
        private Integer label;
        /**
         * 置信度（百分制）
         */
        private Integer prob;
    }
}
