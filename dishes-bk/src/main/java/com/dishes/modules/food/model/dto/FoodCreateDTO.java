package com.dishes.modules.food.model.dto;

import lombok.Data;
import lombok.experimental.Accessors;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 食材创建DTO
 * @author 好市民
 */
@Data
@Accessors(chain = true)
public class FoodCreateDTO {
    /**
     * 厨房ID
     */
    @NotNull(message = "厨房ID不能为空")
    private Long kitchenId;
    /**
     * 食材名称
     */
    @NotBlank(message = "食材名称不能为空")
    private String name;
}
