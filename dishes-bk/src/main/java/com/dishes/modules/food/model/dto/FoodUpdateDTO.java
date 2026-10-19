package com.dishes.modules.food.model.dto;

import lombok.Data;
import lombok.experimental.Accessors;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 食材更新DTO
 * @author 好市民
 */
@Data
@Accessors(chain = true)
public class FoodUpdateDTO {
    /**
     * 食材ID
     */
    @NotNull(message = "食材ID不能为空")
    private Long id;
    /**
     * 食材名称
     */
    @NotBlank(message = "食材名称不能为空")
    private String name;
}
