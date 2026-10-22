package com.think.modules.grp.controller;

import com.think.common.pojo.Result;
import com.think.modules.grp.model.dto.GrpCreateDTO;
import com.think.modules.grp.model.dto.GrpIdDTO;
import com.think.modules.grp.model.dto.GrpJoinDTO;
import com.think.modules.grp.model.dto.GrpMemberPageDTO;
import com.think.modules.grp.model.dto.GrpPageDTO;
import com.think.modules.grp.model.dto.GrpRemoveMemberDTO;
import com.think.modules.grp.model.dto.GrpUpdateDTO;
import com.think.modules.grp.service.GrpService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 群组
 *
 * @author 好市民
 */
@RestController
@RequestMapping("/grp")
public class GrpController {

    @Autowired
    private GrpService grpService;

    /**
     * 我的群列表（分页）
     */
    @PostMapping("/page")
    public Result<?> page(@RequestBody @Validated GrpPageDTO dto) {
        return Result.success(grpService.pageMy(dto));
    }

    /**
     * 建群
     */
    @PostMapping("/create")
    public Result<?> create(@RequestBody @Validated GrpCreateDTO dto) {
        return Result.success(grpService.create(dto));
    }

    /**
     * 群详情
     */
    @GetMapping("/detail")
    public Result<?> detail(@RequestParam Long grpId) {
        return Result.success(grpService.detail(grpId));
    }

    /**
     * 修改群信息（仅群主）
     */
    @PostMapping("/update")
    public Result<?> update(@RequestBody @Validated GrpUpdateDTO dto) {
        grpService.update(dto);
        return Result.success();
    }

    /**
     * 解散群（仅群主）
     */
    @PostMapping("/dissolve")
    public Result<?> dissolve(@RequestBody @Validated GrpIdDTO dto) {
        grpService.dissolve(dto);
        return Result.success();
    }

    /**
     * 生成邀请码
     */
    @PostMapping("/invite")
    public Result<?> invite(@RequestBody @Validated GrpIdDTO dto) {
        return Result.success(grpService.invite(dto));
    }

    /**
     * 凭邀请码入群
     */
    @PostMapping("/join")
    public Result<?> join(@RequestBody @Validated GrpJoinDTO dto) {
        grpService.join(dto);
        return Result.success();
    }

    /**
     * 退出群
     */
    @PostMapping("/leave")
    public Result<?> leave(@RequestBody @Validated GrpIdDTO dto) {
        grpService.leave(dto);
        return Result.success();
    }

    /**
     * 移除群成员（仅群主）
     */
    @PostMapping("/removeMember")
    public Result<?> removeMember(@RequestBody @Validated GrpRemoveMemberDTO dto) {
        grpService.removeMember(dto);
        return Result.success();
    }

    /**
     * 群成员列表（分页）
     */
    @PostMapping("/members/page")
    public Result<?> pageMembers(@RequestBody @Validated GrpMemberPageDTO dto) {
        return Result.success(grpService.pageMembers(dto));
    }
}
