package com.dishes.modules.kitchen.controller;

import com.dishes.common.pojo.Result;
import com.dishes.core.security.utils.SecurityUtils;
import com.dishes.modules.kitchen.model.dto.KitchenCreateDTO;
import com.dishes.modules.kitchen.model.dto.KitchenDeleteDTO;
import com.dishes.modules.kitchen.model.dto.KitchenExpandDTO;
import com.dishes.modules.kitchen.model.dto.KitchenUpdateDTO;
import com.dishes.modules.kitchen.model.vo.KitchenStatsVO;
import com.dishes.modules.kitchen.model.vo.KitchenVO;
import com.dishes.modules.kitchen.service.KitchenService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import org.springframework.beans.factory.annotation.Autowired;
import java.util.List;

/**
 * 厨房
 * @author 好市民
 */
@RestController
@RequestMapping("/kitchen")
public class KitchenController {

    @Autowired
    private KitchenService kitchenService;

    @GetMapping("/list")
    public Result<List<KitchenVO>> list() {
        return Result.success(kitchenService.getList());
    }

    @GetMapping("/info")
    public Result<KitchenVO> info(@RequestParam Long kitchenId) {
        return Result.success(kitchenService.info(kitchenId));
    }

    @GetMapping("/stats")
    public Result<KitchenStatsVO> stats(@RequestParam Long kitchenId) {
        return Result.success(kitchenService.getStats(kitchenId));
    }

    // @PostMapping("/create")
    // public Result<Long> create(@RequestBody @Validated KitchenCreateDTO dto) {
    //     dto.setUserId(SecurityUtils.getLoginUserId());
    //     return Result.success(kitchenService.create(dto));
    // }

    @PostMapping("/update")
    public Result<?> update(@RequestBody @Validated KitchenUpdateDTO dto) {
        kitchenService.update(dto);
        return Result.success();
    }

    @PostMapping("/expand-dish")
    public Result<KitchenVO> expandDish(@RequestBody @Validated KitchenExpandDTO dto) {
        return Result.success(kitchenService.expandDish(dto.getKitchenId()));
    }

    @PostMapping("/expand-category")
    public Result<KitchenVO> expandCategory(@RequestBody @Validated KitchenExpandDTO dto) {
        return Result.success(kitchenService.expandCategory(dto.getKitchenId()));
    }

    @DeleteMapping("/delete")
    public Result<?> delete(@RequestBody @Validated KitchenDeleteDTO dto) {
        kitchenService.delete(dto.getId());
        return Result.success();
    }
}