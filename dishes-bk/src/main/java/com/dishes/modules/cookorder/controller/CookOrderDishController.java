package com.dishes.modules.cookorder.controller;

import com.dishes.modules.cookorder.service.CookOrderDishService;
import org.springframework.web.bind.annotation.*;

import org.springframework.beans.factory.annotation.Autowired;

/**
 * 烹饪订单菜品关联
 * @author 好市民
 */
@RestController
@RequestMapping("/cook-order/dish")
public class CookOrderDishController {

    @Autowired
    private CookOrderDishService cookOrderDishService;

}
