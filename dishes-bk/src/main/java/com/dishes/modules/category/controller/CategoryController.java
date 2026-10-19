package com.dishes.modules.category.controller;

import com.dishes.common.pojo.Result;
import com.dishes.modules.category.model.dto.CategoryCreateDTO;
import com.dishes.modules.category.model.dto.CategoryDeleteDTO;
import com.dishes.modules.category.model.dto.CategorySortDTO;
import com.dishes.modules.category.model.dto.CategoryUpdateDTO;
import com.dishes.modules.category.model.vo.CategoryVO;
import com.dishes.modules.category.service.CategoryService;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import org.springframework.beans.factory.annotation.Autowired;
import java.util.List;

/**
 * 分类
 * @author 好市民
 */
@RestController
@RequestMapping("/category")
public class CategoryController {

    @Autowired
    private CategoryService categoryService;

    @GetMapping("/list")
    public Result<List<CategoryVO>> list(@RequestParam Long kitchenId) {
        return Result.success(categoryService.list(kitchenId));
    }

    @PostMapping("/create")
    public Result<Long> create(@RequestBody @Validated CategoryCreateDTO dto) {
        return Result.success(categoryService.create(dto));
    }

    @PostMapping("/update")
    public Result<?> update(@RequestBody @Validated CategoryUpdateDTO dto) {
        categoryService.update(dto);
        return Result.success();
    }

    @DeleteMapping("/delete")
    public Result<?> delete(@RequestBody @Validated CategoryDeleteDTO dto) {
        categoryService.delete(dto);
        return Result.success();
    }

    @PostMapping("/sort")
    public Result<?> sort(@RequestBody @Validated CategorySortDTO dto) {
        categoryService.sort(dto);
        return Result.success();
    }
}