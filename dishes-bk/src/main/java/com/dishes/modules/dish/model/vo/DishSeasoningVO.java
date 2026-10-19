package com.dishes.modules.dish.model.vo;

import lombok.Data;

/**
 * 菜谱调料项VO
 * @author 好市民
 */
@Data
public class DishSeasoningVO {
    /**
     * 调料ID
     */
    private Long seasoningId;
    /**
     * 调料名称
     */
    private String name;
    /**
     * 用量
     */
    private String amount;
    /**
     * 单位
     */
    private String unit;
    /**
     * 是否可选
     */
    private Integer isOptional;
    /**
     * 排序值
     */
    private Integer sort;
}
