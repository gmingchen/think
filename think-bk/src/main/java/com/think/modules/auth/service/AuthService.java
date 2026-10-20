package com.think.modules.auth.service;

import com.think.modules.auth.model.req.LoginReqVO;
import com.think.modules.auth.model.res.LoginResVO;
import com.think.modules.auth.model.LoginUser;

/**
 * @author 好市民
 */
public interface AuthService {
    /**
     * 小程序登录
     * @param reqVO 登录参数
     * @return
     */
    LoginResVO login(LoginReqVO reqVO);

    /**
     * 获取当前登录的用户
     * @param token
     * @return
     */
    LoginUser queryUser(String token);

    /**
     * 校验token
     * @param token
     * @return
     */
    boolean validateToken(String token);
}
