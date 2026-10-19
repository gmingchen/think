package com.dishes.modules.user.controller;

import com.dishes.common.pojo.Result;
import com.dishes.core.security.utils.SecurityUtils;
import com.dishes.modules.user.model.dto.PresetUserDataDTO;
import com.dishes.modules.user.model.dto.UserQrcodeDTO;
import com.dishes.modules.user.model.dto.UserSetLastKitchenDTO;
import com.dishes.modules.user.model.dto.UserUpdateDTO;
import com.dishes.modules.user.service.UserService;
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

    @PostMapping("/presetData")
    public Result<?> presetUserData(@RequestBody @Validated PresetUserDataDTO dto) {
        userService.presetUserData(dto);
        return Result.success();
    }

    /**
     * 设置上次选择的厨房ID
     * @param dto 参数
     * @return
     */
    @PostMapping("/update/lastKitchen")
    public Result<?> updateLastKitchen(@RequestBody @Validated UserSetLastKitchenDTO dto) {
        userService.updateLastKitchenId(dto);
        return Result.success();
    }

    /**
     * 生成带当前用户 inviterId 参数的小程序码（海报归因）
     * @param dto 参数（page 扫码落地页）
     * @return 小程序码图片 base64 dataURL
     */
    @PostMapping("/generateQrcode")
    public Result<String> generateQrcode(@RequestBody @Validated UserQrcodeDTO dto) {
        return Result.success(userService.generateQrcode(dto.getPage(), SecurityUtils.getLoginUserId()));
    }
}
