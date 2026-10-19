package com.dishes.modules.dish.model.dto;

import lombok.Data;
import lombok.experimental.Accessors;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.List;

/**
 * 菜谱创建DTO
 * @author 好市民
 */
@Data
@Accessors(chain = true)
public class DishCreateDTO {
    /**
     * 厨房ID
     */
    @NotNull(message = "厨房ID不能为空")
    private Long kitchenId;
    /**
     * 分类ID（可选）
     */
    private Long categoryId;
    /**
     * 菜名
     */
    @NotBlank(message = "菜名不能为空")
    private String name;
    /**
     * 封面图URL
     */
    private String cover;
    /**
     * 详情图URL列表（最多 3 张）
     */
    @Size(max = 3, message = "详情图最多 3 张")
    private List<String> images;
    /**
     * 备注
     */
    private String remark;
    /**
     * 食材列表
     */
    private List<DishFoodDTO> foods;
    /**
     * 调料列表
     */
    private List<DishSeasoningDTO> seasonings;
    /**
     * 步骤列表
     */
    private List<DishStepDTO> steps;
}
