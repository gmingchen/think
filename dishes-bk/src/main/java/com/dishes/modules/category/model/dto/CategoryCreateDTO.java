package com.dishes.modules.category.model.dto;

import lombok.Data;
import lombok.experimental.Accessors;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 分类创建DTO
 * @author 好市民
 */
@Data
@Accessors(chain = true)
public class CategoryCreateDTO {
    /**
     * 厨房ID
     */
    @NotNull(message = "厨房ID不能为空")
    private Long kitchenId;
    /**
     * 分类名称
     */
    @NotBlank(message = "分类名称不能为空")
    private String name;
}
