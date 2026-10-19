package com.dishes.modules.dish.controller;

import com.dishes.modules.dish.service.DishSeasoningService;
import org.springframework.web.bind.annotation.*;

import org.springframework.beans.factory.annotation.Autowired;

/**
 * 菜谱调料关联
 * @author 好市民
 */
@RestController
@RequestMapping("/dish/seasoning")
public class DishSeasoningController {

    @Autowired
    private DishSeasoningService dishSeasoningService;

}
