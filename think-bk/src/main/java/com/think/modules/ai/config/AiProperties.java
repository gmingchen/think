package com.think.modules.ai.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * AI 配置
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
         * 每人每日 AI 调用次数上限（与 application.yml base 逐字一致；字段默认值只作漏配时的兜底）
         */
        private Integer dailyLimit = 3;
    }
}
