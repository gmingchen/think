package com.dishes.modules.dish.model.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 菜谱详情VO
 * @author 好市民
 */
@Data
public class DishInfoVO {
    /**
     * 菜谱ID
     */
    private Long id;
    /**
     * 厨房ID
     */
    private Long kitchenId;
    /**
     * 分类ID
     */
    private Long categoryId;
    /**
     * 创建者用户ID
     */
    private Long creatorId;
    /**
     * 菜名
     */
    private String name;
    /**
     * 封面图URL
     */
    private String cover;
    /**
     * 详情图URL列表（最多 3 张）
     */
    private List<String> images;
    /**
     * 备注
     */
    private String remark;
    /**
     * 做过次数
     */
    private Integer cookCount;
    /**
     * 平均评分
     */
    private BigDecimal rating;
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
    /**
     * 食材列表
     */
    private List<DishFoodVO> foods;
    /**
     * 调料列表
     */
    private List<DishSeasoningVO> seasonings;
    /**
     * 步骤列表
     */
    private List<DishStepVO> steps;
}
