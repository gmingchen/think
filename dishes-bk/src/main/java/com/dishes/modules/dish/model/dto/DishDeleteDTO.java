package com.dishes.modules.dish.model.dto;

import lombok.Data;
import lombok.experimental.Accessors;

import javax.validation.constraints.NotEmpty;
import java.util.List;

/**
 * 菜谱删除DTO
 * @author 好市民
 */
@Data
@Accessors(chain = true)
public class DishDeleteDTO {
    /**
     * 菜谱ID列表
     */
    @NotEmpty(message = "菜谱ID列表不能为空")
    private List<Long> ids;
}
