package com.dishes.modules.category.model.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * 分类排序DTO
 * @author 好市民
 */
@Data
public class CategorySortDTO {
    /**
     * 厨房ID
     */
    @NotNull(message = "厨房ID不能为空")
    private Long kitchenId;
    /**
     * 排序列表（按新顺序排列的分类ID）
     */
    @NotNull(message = "排序列表不能为空")
    private List<Long> ids;
}
