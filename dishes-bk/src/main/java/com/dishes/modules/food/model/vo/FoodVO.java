package com.dishes.modules.food.model.vo;

import lombok.Data;

import java.time.LocalDateTime;

/**
 * 食材VO
 * @author 好市民
 */
@Data
public class FoodVO {
    /**
     * ID
     */
    private Long id;
    /**
     * 食材名称
     */
    private String name;
    /**
     * 使用次数
     */
    private Integer usageCount;
    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
}
