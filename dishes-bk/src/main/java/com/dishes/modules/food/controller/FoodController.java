package com.dishes.modules.food.controller;

import com.dishes.common.pojo.Result;
import com.dishes.modules.food.model.dto.FoodCreateDTO;
import com.dishes.modules.food.model.dto.FoodDeleteDTO;
import com.dishes.modules.food.model.dto.FoodUpdateDTO;
import com.dishes.modules.food.model.vo.FoodVO;
import com.dishes.modules.food.service.FoodService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import org.springframework.beans.factory.annotation.Autowired;
import java.util.List;

/**
 * 食材
 * @author 好市民
 */
@RestController
@RequestMapping("/food")
public class FoodController {

    @Autowired
    private FoodService foodService;

    @GetMapping("/list")
    public Result<List<FoodVO>> list(@RequestParam Long kitchenId) {
        return Result.success(foodService.listVO(kitchenId));
    }

    @PostMapping("/create")
    public Result<Long> create(@RequestBody @Validated FoodCreateDTO dto) {
        return Result.success(foodService.create(dto));
    }

    @PostMapping("/update")
    public Result<?> update(@RequestBody @Validated FoodUpdateDTO dto) {
        foodService.update(dto);
        return Result.success();
    }

    @DeleteMapping("/delete")
    public Result<?> delete(@RequestBody @Validated FoodDeleteDTO dto) {
        foodService.delete(dto);
        return Result.success();
    }
}