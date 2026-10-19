package com.dishes.modules.dish.model.dto;

import lombok.Data;
import lombok.experimental.Accessors;

import javax.validation.constraints.NotNull;

/**
 * 菜谱收藏DTO
 * @author 好市民
 */
@Data
@Accessors(chain = true)
public class DishCollectDTO {
    /**
     * 源菜谱ID
     */
    @NotNull(message = "菜谱ID不能为空")
    private Long dishId;
    /**
     * 目标厨房ID
     */
    @NotNull(message = "厨房ID不能为空")
    private Long kitchenId;
}
