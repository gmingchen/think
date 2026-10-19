package com.dishes.modules.dish.model.dto;

import lombok.Data;
import lombok.experimental.Accessors;

import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * 菜谱食材保存DTO
 * @author 好市民
 */
@Data
@Accessors(chain = true)
public class DishFoodSaveUpdateDTO {
    /**
     * 菜谱ID
     */
    @NotNull(message = "菜谱ID不能为空")
    private Long id;
    /**
     * 食材列表
     */
    private List<DishFoodDTO> list;
}
