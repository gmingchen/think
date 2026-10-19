package com.dishes.modules.dish.controller;

import com.dishes.modules.dish.service.DishStepService;
import org.springframework.web.bind.annotation.*;

import org.springframework.beans.factory.annotation.Autowired;

/**
 * 菜谱步骤
 * @author 好市民
 */
@RestController
@RequestMapping("/dish/step")
public class DishStepController {

    @Autowired
    private DishStepService dishStepService;

}
