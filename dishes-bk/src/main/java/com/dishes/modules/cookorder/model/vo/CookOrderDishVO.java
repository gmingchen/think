package com.dishes.modules.cookorder.model.vo;

import lombok.Data;

/**
 * 烹饪订单菜品VO
 * @author 好市民
 */
@Data
public class CookOrderDishVO {
    /**
     * 菜品ID
     */
    private Long dishId;
    /**
     * 菜品名称快照
     */
    private String dishName;
    /**
     * 菜品封面快照
     */
    private String dishCover;
    /**
     * 数量
     */
    private Integer quantity;
    /**
     * 评分（已评价时返回）
     */
    private Integer rating;
    /**
     * 文字评价（已评价时返回）
     */
    private String content;
}
