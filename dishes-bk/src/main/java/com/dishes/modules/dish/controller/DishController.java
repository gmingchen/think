package com.dishes.modules.dish.controller;

import com.dishes.common.pojo.Result;
import com.dishes.modules.dish.model.dto.DishBatchUpdateCategoryDTO;
import com.dishes.modules.dish.model.dto.DishCollectDTO;
import com.dishes.modules.dish.model.dto.DishCreateDTO;
import com.dishes.modules.dish.model.dto.DishDeleteDTO;
import com.dishes.modules.dish.model.dto.DishShoppingListDTO;
import com.dishes.modules.dish.model.dto.DishSortDTO;
import com.dishes.modules.dish.model.dto.DishUpdateDTO;
import com.dishes.modules.dish.model.vo.DishInfoVO;
import com.dishes.modules.dish.model.vo.DishShoppingListVO;
import com.dishes.modules.dish.model.vo.DishShareVO;
import com.dishes.modules.dish.model.vo.DishVO;
import com.dishes.modules.dish.service.DishService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import org.springframework.beans.factory.annotation.Autowired;
import java.util.List;

/**
 * 菜谱
 * @author 好市民
 */
@RestController
@RequestMapping("/dish")
public class DishController {

    @Autowired
    private DishService dishService;

    @GetMapping("/list")
    public Result<List<DishVO>> list(@RequestParam Long kitchenId) {
        return Result.success(dishService.list(kitchenId));
    }

    @GetMapping("/list/category")
    public Result<List<DishVO>> listByCategory(@RequestParam Long kitchenId,
                                               @RequestParam(required = false) Long categoryId) {
        return Result.success(dishService.listByCategory(kitchenId, categoryId));
    }

    @GetMapping("/best")
    public Result<List<DishVO>> best(@RequestParam Long kitchenId) {
        return Result.success(dishService.best(kitchenId));
    }

    @GetMapping("/info")
    public Result<DishInfoVO> info(@RequestParam Long id) {
        return Result.success(dishService.info(id));
    }

    @PostMapping("/shopping/list")
    public Result<List<DishShoppingListVO>> shoppingList(@RequestBody @Validated DishShoppingListDTO dto) {
        return Result.success(dishService.shoppingList(dto));
    }

    @PostMapping("/create")
    public Result<Long> create(@RequestBody @Validated DishCreateDTO dto) {
        return Result.success(dishService.create(dto));
    }

    @PostMapping("/update")
    public Result<?> update(@RequestBody @Validated DishUpdateDTO dto) {
        dishService.update(dto);
        return Result.success();
    }

    @DeleteMapping("/delete/batch")
    public Result<?> delete(@RequestBody @Validated DishDeleteDTO dto) {
        dishService.delete(dto);
        return Result.success();
    }

    @PostMapping("/update/category/batch")
    public Result<?> batchUpdateCategory(@RequestBody @Validated DishBatchUpdateCategoryDTO dto) {
        dishService.batchUpdateCategory(dto);
        return Result.success();
    }

    @PostMapping("/sort")
    public Result<?> sort(@RequestBody @Validated DishSortDTO dto) {
        dishService.sort(dto);
        return Result.success();
    }

    @GetMapping("/share/info")
    public Result<DishShareVO> shareInfo(@RequestParam Long id) {
        return Result.success(dishService.shareInfo(id));
    }

    @PostMapping("/collect")
    public Result<?> collect(@RequestBody @Validated DishCollectDTO dto) {
        dishService.collect(dto);
        return Result.success();
    }
}