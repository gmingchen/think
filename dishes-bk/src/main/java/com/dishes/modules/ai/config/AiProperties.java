package com.dishes.modules.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * AI 荐菜配置
 * <p>
 * 赠送额度仅限小程序 SDK / 云函数消耗，故 llmUrl 指向 CloudBase 云函数 llm-proxy 的 HTTP 访问地址；
 * proxyKey 为后端与云函数之间的共享密钥（仅存服务器配置，不进 Git）
 * </p>
 * @author 好市民
 */
@Data
@Component
@ConfigurationProperties(prefix = "ai")
public class AiProperties {

    /**
     * LLM 中转配置
     */
    private Llm llm = new Llm();

    /**
     * AI 封面生图中转配置（云函数 image-proxy，与 llm-proxy 同形态，共用 PROXY_KEY）
     */
    private Image image = new Image();

    @Data
    public static class Llm {
        /**
         * 云函数 HTTP 访问地址（为空视为未开通 AI 能力）
         */
        private String url;
        /**
         * 与云函数共享的鉴权密钥
         */
        private String proxyKey;
        /**
         * 模型名（云函数内部消费用，透传字段）
         */
        private String model = "hy3";
        /**
         * 生成温度
         */
        private Double temperature = 0.7;
        /**
         * 请求超时（毫秒），LLM 生成耗时远大于常规接口
         */
        private Integer timeout = 90000;
        /**
         * 每人每日推荐次数上限（与 application.yml base 逐字一致；字段默认值只作漏配时的兜底，
         * 不一致会让某个 profile 漏配时静默掉到另一个数，改 base 必同时改这里）
         */
        private Integer dailyLimit = 3;
        /**
         * 每人每日文案识别次数上限（独立预算，仅 parse 计数；与 base 一致）
         */
        private Integer parseDailyLimit = 5;
        /**
         * 畅厨会员每人每日推荐次数上限（轻享不加量、与基础上限同值；§6.5 畅厨分层 20/30/10）
         * 与 application.yml base 逐字一致，改 base 必同时改这里
         */
        private Integer proDailyLimit = 20;
        /**
         * 畅厨会员每人每日文案识别次数上限（轻享不加量；与 base 一致）
         */
        private Integer proParseDailyLimit = 30;
        /**
         * 推荐结果 Redis 暂存过期时间（秒），即采纳凭证有效期
         */
        private Long recommendExpire = 86400L;
    }

    @Data
    public static class Image {
        /**
         * 云函数 image-proxy 的 HTTP 访问地址（为空视为未开通 AI 生图能力）
         */
        private String url;
        /**
         * 与云函数共享的鉴权密钥（与 llm.proxyKey 同值即可，两函数共用 PROXY_KEY）
         */
        private String proxyKey;
        /**
         * 生图模型名（云函数内部消费，本字段仅展示用，不传；云函数固定 HY-Image-3.0-Plus-4090-Tob-v1.0）
         */
        private String model = "HY-Image-3.0-Plus-4090-Tob-v1.0";
        /**
         * 图片尺寸（混元只认「宽x高」格式：1024x1024 / 1280x720 / 720x1280 / 1280x1280，本项固定方形与菜谱封面比例一致）
         */
        private String size = "1024x1024";
        /**
         * 单次生图调用超时（毫秒），需 ≥ 云函数侧 120s
         */
        private Integer timeout = 130000;
        /**
         * 每人每日生图次数上限（独立预算，不与荐菜/识别挤占；与 base 一致）
         */
        private Integer dailyLimit = 2;
        /**
         * 畅厨会员每人每日生图次数上限（轻享不加量；生图有真实赠额成本，提额即转嫁成本，§6.5；与 base 一致）
         */
        private Integer proDailyLimit = 10;
    }
}
