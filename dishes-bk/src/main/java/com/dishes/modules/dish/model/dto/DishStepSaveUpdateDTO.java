package com.dishes.modules.dish.model.dto;

import lombok.Data;
import lombok.experimental.Accessors;

import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * 菜谱步骤保存DTO
 * @author 好市民
 */
@Data
@Accessors(chain = true)
public class DishStepSaveUpdateDTO {
    /**
     * 菜谱ID
     */
    @NotNull(message = "菜谱ID不能为空")
    private Long id;
    /**
     * 步骤列表
     */
    private List<DishStepDTO> list;
}
