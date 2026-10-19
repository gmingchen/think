package com.dishes.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 厨房扩容字段枚举
 * <p>约束看视频扩容可自增的列，列名为编译期固定常量，避免 SQL 拼接魔法字符串与注入风险
 * @author 好市民
 */
@Getter
@AllArgsConstructor
public enum KitchenExpandFieldEnum {
    /**
     * 扩容维度
     */
    DISH("max_dish_count", "菜谱"),
    CATEGORY("max_category_count", "分类"),
    ;

    /**
     * 对应 kitchen 表的物理列名
     */
    private final String column;
    /**
     * 维度名称（用于提示文案）
     */
    private final String message;
}
