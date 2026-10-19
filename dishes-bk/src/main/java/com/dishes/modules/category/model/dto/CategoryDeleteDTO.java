package com.dishes.modules.category.model.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * 分类删除DTO
 * @author 好市民
 */
@Data
public class CategoryDeleteDTO {
    /**
     * 分类ID
     */
    @NotNull(message = "分类ID不能为空")
    private Long id;
    /**
     * 目标分类ID（该分类下的菜谱迁移到此分类，为空则不迁移）
     */
    private Long targetCategoryId;
}
