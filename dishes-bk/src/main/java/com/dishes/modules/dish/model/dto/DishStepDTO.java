package com.dishes.modules.dish.model.dto;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 菜谱步骤DTO
 * @author 好市民
 */
@Data
@Accessors(chain = true)
public class DishStepDTO {
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
