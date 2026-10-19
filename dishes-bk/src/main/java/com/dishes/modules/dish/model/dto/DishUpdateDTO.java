package com.dishes.modules.dish.model.dto;

import lombok.Data;
import lombok.experimental.Accessors;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.util.List;

/**
 * 菜谱更新DTO
 * @author 好市民
 */
@Data
@Accessors(chain = true)
public class DishUpdateDTO {
    /**
     * 菜谱ID
     */
    @NotNull(message = "菜谱ID不能为空")
    private Long id;
    /**
     * 分类ID
     */
    private Long categoryId;
    /**
     * 菜名
     */
    private String name;
    /**
     * 封面图URL
     */
    private String cover;
    /**
     * 详情图URL列表（最多 3 张，全量替换）
     */
    @Size(max = 3, message = "详情图最多 3 张")
    private List<String> images;
    /**
     * 备注
     */
    private String remark;
    /**
     * 食材列表（全量替换）
     */
    private List<DishFoodDTO> foods;
    /**
     * 调料列表（全量替换）
     */
    private List<DishSeasoningDTO> seasonings;
    /**
     * 步骤列表（全量替换）
     */
    private List<DishStepDTO> steps;
}
