package com.dishes.modules.kitchen.controller;

import com.dishes.common.pojo.Result;
import com.dishes.modules.kitchen.model.dto.KitchenUserDeleteDTO;
import com.dishes.modules.kitchen.model.dto.KitchenUserInviteDTO;
import com.dishes.modules.kitchen.model.dto.KitchenUserJoinDTO;
import com.dishes.modules.kitchen.model.dto.KitchenUserLeaveDTO;
import com.dishes.modules.kitchen.model.dto.KitchenUserUpdateRoleDTO;
import com.dishes.modules.kitchen.model.vo.KitchenInviteInfoVO;
import com.dishes.modules.kitchen.model.vo.KitchenUserVO;
import com.dishes.modules.kitchen.service.KitchenUserService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import org.springframework.beans.factory.annotation.Autowired;
import java.util.List;

/**
 * 厨房成员
 * @author 好市民
 */
@RestController
@RequestMapping("/kitchen/user")
public class KitchenUserController {

    @Autowired
    private KitchenUserService kitchenUserService;

    @GetMapping("/list")
    public Result<List<KitchenUserVO>> list(@RequestParam Long kitchenId) {
        return Result.success(kitchenUserService.list(kitchenId));
    }

    @PostMapping("/update/role")
    public Result<?> updateRole(@RequestBody @Validated KitchenUserUpdateRoleDTO dto) {
        kitchenUserService.updateRole(dto);
        return Result.success();
    }

    @DeleteMapping("/delete")
    public Result<?> delete(@RequestBody @Validated KitchenUserDeleteDTO dto) {
        kitchenUserService.delete(dto);
        return Result.success();
    }

    @PostMapping("/invite")
    public Result<String> invite(@RequestBody @Validated KitchenUserInviteDTO dto) {
        return Result.success(kitchenUserService.invite(dto));
    }

    @GetMapping("/invite/info")
    public Result<KitchenInviteInfoVO> inviteInfo(@RequestParam Long kitchenId, @RequestParam String code) {
        return Result.success(kitchenUserService.getInviteInfo(kitchenId, code));
    }

    @PostMapping("/join")
    public Result<?> join(@RequestBody @Validated KitchenUserJoinDTO dto) {
        kitchenUserService.join(dto);
        return Result.success();
    }

    @PostMapping("/leave")
    public Result<?> leave(@RequestBody @Validated KitchenUserLeaveDTO dto) {
        kitchenUserService.leave(dto.getKitchenId());
        return Result.success();
    }
}
