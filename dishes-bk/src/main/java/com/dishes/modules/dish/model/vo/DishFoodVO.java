package com.dishes.modules.dish.model.vo;

import lombok.Data;

/**
 * 菜谱食材项VO
 * @author 好市民
 */
@Data
public class DishFoodVO {
    /**
     * 食材ID
     */
    private Long foodId;
    /**
     * 食材名称
     */
    private String foodName;
    /**
     * 用量
     */
    private String amount;
    /**
     * 单位
     */
    private String unit;
    /**
     * 处理方式
     */
    private String process;
    /**
     * 是否可选
     */
    private Integer isOptional;
    /**
     * 排序值
     */
    private Integer sort;
}
