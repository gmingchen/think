package com.think.modules.decision.controller;

import com.think.common.pojo.Result;
import com.think.modules.decision.model.dto.DecisionCreateDTO;
import com.think.modules.decision.model.dto.DecisionGrpPageDTO;
import com.think.modules.decision.model.dto.DecisionIdDTO;
import com.think.modules.decision.model.dto.DecisionPageDTO;
import com.think.modules.decision.model.dto.DecisionPlazaPageDTO;
import com.think.modules.decision.model.dto.DecisionPublishDTO;
import com.think.modules.decision.model.dto.DecisionUpdateDTO;
import com.think.modules.decision.service.DecisionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 决策单
 *
 * @author 好市民
 */
@RestController
@RequestMapping("/decision")
public class DecisionController {

    @Autowired
    private DecisionService decisionService;

    // ================= 读 =================

    /**
     * 我发起的决策单列表
     */
    @PostMapping("/pageMine")
    public Result<?> pageMine(@RequestBody @Validated DecisionPageDTO dto) {
        return Result.success(decisionService.pageMine(dto));
    }

    /**
     * 我评议过的决策单列表
     */
    @PostMapping("/pageReviewed")
    public Result<?> pageReviewed(@RequestBody @Validated DecisionPageDTO dto) {
        return Result.success(decisionService.pageReviewed(dto));
    }

    /**
     * 群内决策单列表
     */
    @PostMapping("/pageGrp")
    public Result<?> pageGrp(@RequestBody @Validated DecisionGrpPageDTO dto) {
        return Result.success(decisionService.pageGrp(dto));
    }

    /**
     * 公开广场列表
     */
    @PostMapping("/pagePlaza")
    public Result<?> pagePlaza(@RequestBody @Validated DecisionPlazaPageDTO dto) {
        return Result.success(decisionService.pagePlaza(dto));
    }

    /**
     * 随机查看一条公开决策单（首次「去看看」）
     */
    @GetMapping("/randomPlaza")
    public Result<?> randomPlaza() {
        return Result.success(decisionService.randomPlaza());
    }

    /**
     * 换一个：随机取一条且保证不与 excludeId（上一条）重复；无可换时 data 为 null
     */
    @GetMapping("/changePlaza")
    public Result<?> changePlaza(@RequestParam(required = false) Long excludeId) {
        return Result.success(decisionService.changePlaza(excludeId));
    }

    /**
     * 决策单详情
     */
    @GetMapping("/detail")
    public Result<?> detail(@RequestParam Long id) {
        return Result.success(decisionService.detail(id));
    }

    /**
     * 我的统计
     */
    @GetMapping("/statsMine")
    public Result<?> statsMine() {
        return Result.success(decisionService.statsMine());
    }

    // ================= 写 =================

    /**
     * 创建决策单（存草稿）
     */
    @PostMapping("/create")
    public Result<?> create(@RequestBody @Validated DecisionCreateDTO dto) {
        return Result.success(decisionService.create(dto));
    }

    /**
     * 编辑决策单（草稿/审核退回态）
     */
    @PostMapping("/update")
    public Result<?> update(@RequestBody @Validated DecisionUpdateDTO dto) {
        decisionService.update(dto);
        return Result.success();
    }

    /**
     * 发布决策单
     */
    @PostMapping("/publish")
    public Result<?> publish(@RequestBody @Validated DecisionPublishDTO dto) {
        decisionService.publish(dto);
        return Result.success();
    }

    /**
     * 提前结束评议
     */
    @PostMapping("/endEarly")
    public Result<?> endEarly(@RequestBody @Validated DecisionIdDTO dto) {
        decisionService.endEarly(dto);
        return Result.success();
    }

    /**
     * 关闭评议
     */
    @PostMapping("/close")
    public Result<?> close(@RequestBody @Validated DecisionIdDTO dto) {
        decisionService.close(dto);
        return Result.success();
    }

    /**
     * 删除决策单
     */
    @PostMapping("/delete")
    public Result<?> delete(@RequestBody @Validated DecisionIdDTO dto) {
        decisionService.delete(dto);
        return Result.success();
    }
}
