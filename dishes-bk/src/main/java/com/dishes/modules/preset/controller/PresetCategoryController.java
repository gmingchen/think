package com.dishes.modules.preset.controller;

import com.dishes.common.pojo.Result;
import com.dishes.modules.preset.model.vo.PresetItemVO;
import com.dishes.modules.preset.service.PresetCategoryService;
import org.springframework.web.bind.annotation.*;

import org.springframework.beans.factory.annotation.Autowired;
import java.util.List;

/**
 * 预设分类
 * @author 好市民
 */
@RestController
@RequestMapping("/preset/category")
public class PresetCategoryController {

    @Autowired
    private PresetCategoryService presetCategoryService;

    @GetMapping("/list")
    public Result<List<PresetItemVO>> list() {
        return Result.success(presetCategoryService.getList());
    }

}
