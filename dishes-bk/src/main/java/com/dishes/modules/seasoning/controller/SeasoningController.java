package com.dishes.modules.seasoning.controller;

import com.dishes.common.pojo.Result;
import com.dishes.modules.seasoning.model.dto.SeasoningCreateDTO;
import com.dishes.modules.seasoning.model.dto.SeasoningDeleteDTO;
import com.dishes.modules.seasoning.model.dto.SeasoningUpdateDTO;
import com.dishes.modules.seasoning.model.vo.SeasoningVO;
import com.dishes.modules.seasoning.service.SeasoningService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import org.springframework.beans.factory.annotation.Autowired;
import java.util.List;

/**
 * 调料
 * @author 好市民
 */
@RestController
@RequestMapping("/seasoning")
public class SeasoningController {

    @Autowired
    private SeasoningService seasoningService;

    @GetMapping("/list")
    public Result<List<SeasoningVO>> list(@RequestParam Long kitchenId) {
        return Result.success(seasoningService.listVO(kitchenId));
    }

    @PostMapping("/create")
    public Result<Long> create(@RequestBody @Validated SeasoningCreateDTO dto) {
        return Result.success(seasoningService.create(dto));
    }

    @PostMapping("/update")
    public Result<?> update(@RequestBody @Validated SeasoningUpdateDTO dto) {
        seasoningService.update(dto);
        return Result.success();
    }

    @DeleteMapping("/delete")
    public Result<?> delete(@RequestBody @Validated SeasoningDeleteDTO dto) {
        seasoningService.delete(dto);
        return Result.success();
    }
}