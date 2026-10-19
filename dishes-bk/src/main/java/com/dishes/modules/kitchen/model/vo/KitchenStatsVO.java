package com.dishes.modules.kitchen.model.vo;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 厨房统计VO
 * @author 好市民
 */
@Data
@Accessors(chain = true)
public class KitchenStatsVO {
    /**
     * 分类数
     */
    private Integer categoryCount;
    /**
     * 菜谱数
     */
    private Integer dishCount;
    /**
     * 下厨次数
     */
    private Integer cookOrderCount;
    /**
     * 盛盘菜谱数
     */
    private Integer cookOrderDishCount;
    /**
     * 食材数
     */
    private Integer foodCount;
    /**
     * 调料数
     */
    private Integer seasoningCount;
    /**
     * 成员数
     */
    private Integer memberCount;
}
