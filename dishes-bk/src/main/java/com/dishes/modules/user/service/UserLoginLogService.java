package com.dishes.modules.user.service;

import com.dishes.core.mybatisplus.service.IServiceX;
import com.dishes.modules.user.entity.UserLoginLogEntity;

/**
 * 用户登录流水（v14，DAU 精确统计源）
 * @author 好市民
 */
public interface UserLoginLogService extends IServiceX<UserLoginLogEntity> {

    /**
     * 记录一次登录流水。由登录流程调用，每次小程序登录落一行，
     * 日活按 {@code COUNT(DISTINCT user_id)} 聚合，替代 last_login_at 单值覆盖的近似口径。
     *
     * @param userId 登录用户ID
     * @param openid 微信openid
     */
    void record(Long userId, String openid);
}
