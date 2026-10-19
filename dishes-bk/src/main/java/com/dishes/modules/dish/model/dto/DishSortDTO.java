package com.dishes.modules.dish.model.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * 菜谱排序DTO
 * @author 好市民
 */
@Data
public class DishSortDTO {
    /**
     * 厨房ID
     */
    @NotNull(message = "厨房ID不能为空")
    private Long kitchenId;
    /**
     * 分类ID（可选，为空表示对"未分类"组排序）
     */
    private Long categoryId;
    /**
     * 排序列表（按新顺序排列的菜谱ID）
     */
    @NotNull(message = "排序列表不能为空")
    private List<Long> ids;
}
