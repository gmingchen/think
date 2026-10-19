package com.dishes.modules.cookorder.model.dto;

import lombok.Data;
import lombok.experimental.Accessors;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * 烹饪订单菜谱DTO
 * @author 好市民
 */
@Data
@Accessors(chain = true)
public class CookOrderDishDTO {
    /**
     * 菜谱ID
     */
    @NotNull(message = "菜谱ID不能为空")
    private Long dishId;
    /**
     * 数量
     */
    @NotNull(message = "数量不能为空")
    private Integer quantity;
}
