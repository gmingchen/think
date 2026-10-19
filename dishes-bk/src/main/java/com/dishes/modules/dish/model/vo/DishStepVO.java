package com.dishes.modules.dish.model.vo;

import lombok.Data;

/**
 * 菜谱步骤项VO
 * @author 好市民
 */
@Data
public class DishStepVO {
    /**
     * 步骤描述
     */
    private String description;
    /**
     * 步骤图URL
     */
    private String image;
    /**
     * 排序值
     */
    private Integer sort;
}
