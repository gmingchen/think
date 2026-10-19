package com.dishes.modules.dish.controller;

import com.dishes.modules.dish.service.DishFoodService;
import org.springframework.web.bind.annotation.*;

import org.springframework.beans.factory.annotation.Autowired;

/**
 * 菜谱食材关联
 * @author 好市民
 */
@RestController
@RequestMapping("/dish/food")
public class DishFoodController {

    @Autowired
    private DishFoodService dishFoodService;

}
