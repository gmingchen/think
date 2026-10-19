package com.dishes.modules.dish.model.vo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 菜谱列表VO
 * @author 好市民
 */
@Data
public class DishVO {
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
     * 菜名
     */
    private String name;
    /**
     * 封面图URL
     */
    private String cover;
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
     * 排序值（同厨房同分类内手动升序）
     */
    private Integer sort;
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
}
