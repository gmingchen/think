package com.dishes.modules.wechat.xpay.config;

import org.springframework.context.EnvironmentAware;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

import java.util.Arrays;

/**
 * 虚拟支付环境标识（按当前 profile 现算，不再有 yml 配置项 / DB 列这第二处真相）
 * <p>
 * 设计约定：prod profile = 现网(env=0)，其余 profile（dev / test）= 沙箱(env=1)。
 * 微信 {@code requestVirtualPayment} 的 signData 与 {@code query_order} 请求体都必须带 env(0/1)，
 * 这个数不能不发，所以在组装时按 profile 现算，而不是让 yml 与 profile 各存一份、再靠启动校验去对齐它们
 * （对齐本身就是一种会写错的双头状态）。
 * </p>
 * <p>
 * ⚠️ 本值是「下单 env」编码（0-现网 1-沙箱），与查单响应里的 {@code env_type}（1-现网 2-沙箱）不是同一套，别直接比较。
 * </p>
 *
 * @author 好市民
 */
@Component
public class XpayEnv implements EnvironmentAware {

    /**
     * 现网
     */
    public static final int ENV_PROD = 0;
    /**
     * 沙箱
     */
    public static final int ENV_SANDBOX = 1;

    /**
     * 唯一决定 env 的 profile：命中即现网，其余一律沙箱
     */
    private static final String PROFILE_PROD = "prod";

    private boolean prod;

    @Override
    public void setEnvironment(Environment environment) {
        this.prod = Arrays.asList(environment.getActiveProfiles()).contains(PROFILE_PROD);
    }

    /**
     * 当前环境的微信 env 值：prod → 0（现网），否则 → 1（沙箱）
     */
    public int get() {
        return prod ? ENV_PROD : ENV_SANDBOX;
    }

    /**
     * 是否为现网环境
     */
    public boolean isProd() {
        return prod;
    }
}
