package com.dishes.modules.cookreview.model.dto;

import lombok.Data;
import lombok.experimental.Accessors;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

/**
 * 评价菜谱DTO
 * @author 好市民
 */
@Data
@Accessors(chain = true)
public class CookReviewDishDTO {
    /**
     * 菜谱ID
     */
    @NotNull(message = "菜谱ID不能为空")
    private Long dishId;
    /**
     * 评分（1-5）
     */
    @NotNull(message = "评分不能为空")
    @Min(value = 1, message = "评分最小为1")
    @Max(value = 5, message = "评分最大为5")
    private Integer rating;
    /**
     * 文字评价
     */
    private String content;
}
