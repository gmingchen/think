package com.dishes.modules.category.model.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 分类更新DTO
 * @author 好市民
 */
@Data
public class CategoryUpdateDTO {
    /**
     * 分类ID
     */
    @NotNull(message = "分类ID不能为空")
    private Long id;
    /**
     * 分类名称
     */
    @NotBlank(message = "分类名称不能为空")
    private String name;
}
