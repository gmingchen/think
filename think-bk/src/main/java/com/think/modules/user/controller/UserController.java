package com.think.modules.user.controller;

import com.think.common.pojo.Result;
import com.think.modules.user.model.dto.UserUpdateDTO;
import com.think.modules.user.service.UserService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import org.springframework.beans.factory.annotation.Autowired;

/**
 * 用户
 * @author 好市民
 */
@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserService userService;

    /**
     * 编辑用户信息
     * @param dto 参数
     * @return
     */
    @PostMapping("/update")
    public Result<?> update(@RequestBody @Validated UserUpdateDTO dto) {
        userService.update(dto);
        return Result.success();
    }
}
