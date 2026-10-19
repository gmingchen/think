package com.dishes.modules.preset.controller;

import com.dishes.common.pojo.Result;
import com.dishes.modules.preset.model.vo.GroupVO;
import com.dishes.modules.preset.model.vo.PresetItemVO;
import com.dishes.modules.preset.service.PresetFoodService;
import org.springframework.web.bind.annotation.*;

import org.springframework.beans.factory.annotation.Autowired;
import java.util.List;

/**
 * 预设食材
 * @author 好市民
 */
@RestController
@RequestMapping("/preset/food")
public class PresetFoodController {

    @Autowired
    private PresetFoodService presetFoodService;

    @GetMapping("/list")
    public Result<List<GroupVO<PresetItemVO>>> list() {
        return Result.success(presetFoodService.getGroupedList());
    }

}
