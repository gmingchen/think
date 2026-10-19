package com.dishes.modules.dish.model.dto;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 菜谱食材DTO
 * @author 好市民
 */
@Data
@Accessors(chain = true)
public class DishFoodDTO {
    /**
     * 食材ID
     */
    private Long foodId;
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
