package com.dishes.modules.food.model.dto;

import lombok.Data;
import lombok.experimental.Accessors;

import javax.validation.constraints.NotNull;

/**
 * 食材删除DTO
 * @author 好市民
 */
@Data
@Accessors(chain = true)
public class FoodDeleteDTO {
    /**
     * 食材ID
     */
    @NotNull(message = "食材ID不能为空")
    private Long id;
}
