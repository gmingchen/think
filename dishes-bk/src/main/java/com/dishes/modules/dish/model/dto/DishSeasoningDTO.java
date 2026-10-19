package com.dishes.modules.dish.model.dto;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 菜谱调料DTO
 * @author 好市民
 */
@Data
@Accessors(chain = true)
public class DishSeasoningDTO {
    /**
     * 调料ID
     */
    private Long seasoningId;
    /**
     * 用量
     */
    private String amount;
    /**
     * 单位
     */
    private String unit;
    /**
     * 是否可选
     */
    private Integer isOptional;
    /**
     * 排序值
     */
    private Integer sort;
}
