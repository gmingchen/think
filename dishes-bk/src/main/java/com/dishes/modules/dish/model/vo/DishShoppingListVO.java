package com.dishes.modules.dish.model.vo;

import lombok.Data;
import lombok.experimental.Accessors;

import java.util.List;

/**
 * 采购清单菜品项VO（单道菜的食材/调料明细，聚合合并由前端完成）
 * @author 好市民
 */
@Data
@Accessors(chain = true)
public class DishShoppingListVO {
    /**
     * 菜谱ID
     */
    private Long dishId;
    /**
     * 菜名
     */
    private String dishName;
    /**
     * 菜谱封面（清单页来源菜列表展示用）
     */
    private String dishCover;
    /**
     * 份数
     */
    private Integer quantity;
    /**
     * 该菜来自哪些订单 id（dishIds 路径为空；前端勾选缓存按订单分片存储用）
     */
    private List<Long> cookOrderIds;
    /**
     * 食材列表
     */
    private List<DishFoodVO> foods;
    /**
     * 调料列表
     */
    private List<DishSeasoningVO> seasonings;
}
