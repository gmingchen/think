package com.dishes.modules.category.model.vo;

import lombok.Data;

/**
 * 分类VO
 * @author 好市民
 */
@Data
public class CategoryVO {
    /**
     * ID
     */
    private Long id;
    /**
     * 厨房ID
     */
    private Long kitchenId;
    /**
     * 分类名称
     */
    private String name;
    /**
     * 排序值
     */
    private Integer sort;
}
