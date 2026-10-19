package com.dishes.modules.auth.controller;

import com.dishes.common.pojo.Result;
import com.dishes.core.repeat.annotation.Repeat;
import com.dishes.core.security.utils.SecurityUtils;
import com.dishes.modules.auth.model.req.LoginReqVO;
import com.dishes.modules.auth.model.res.LoginResVO;
import com.dishes.modules.auth.service.AuthService;
import com.dishes.modules.auth.model.LoginUser;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import org.springframework.beans.factory.annotation.Autowired;

/**
 * @author 好市民
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private AuthService appAuthService;

    /**
     * 小程序登录
     * @param reqVO 参数
     * @return
     */
    @Repeat
    @PostMapping("/login")
    public Result<LoginResVO> systemLogin(@RequestBody @Validated LoginReqVO reqVO) {
        return Result.success(
                appAuthService.login(reqVO)
        );
    }

    /**
     * 获取当前登录的用户信息
     * @return
     */
    @GetMapping("/info")
    public Result<LoginUser> userInfo() {
        return Result.success(SecurityUtils.getLoginUser());
    }
}
