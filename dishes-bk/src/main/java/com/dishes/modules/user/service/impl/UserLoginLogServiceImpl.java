package com.dishes.modules.user.service.impl;

import com.dishes.core.mybatisplus.service.ServiceImplX;
import com.dishes.modules.user.entity.UserLoginLogEntity;
import com.dishes.modules.user.mapper.UserLoginLogMapper;
import com.dishes.modules.user.service.UserLoginLogService;
import org.springframework.stereotype.Service;

/**
 * 用户登录流水（v14）
 * @author 好市民
 */
@Service("userLoginLogService")
public class UserLoginLogServiceImpl extends ServiceImplX<UserLoginLogMapper, UserLoginLogEntity> implements UserLoginLogService {

    @Override
    public void record(Long userId, String openid) {
        // created_at 由 AutoFillHandler 在 INSERT 时填充，即登录时刻
        baseMapper.insert(new UserLoginLogEntity()
                .setUserId(userId)
                .setOpenid(openid));
    }
}
