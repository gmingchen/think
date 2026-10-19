package com.dishes.modules.dish.model.dto;

import lombok.Data;
import lombok.experimental.Accessors;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * 菜谱批量修改分类DTO
 * @author 好市民
 */
@Data
@Accessors(chain = true)
public class DishBatchUpdateCategoryDTO {
    /**
     * 菜谱ID列表
     */
    @NotEmpty(message = "菜谱ID列表不能为空")
    private List<Long> ids;
    /**
     * 分类ID
     */
    @NotNull(message = "分类ID不能为空")
    private Long categoryId;
}
