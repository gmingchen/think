package com.dishes.modules.kitchen.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.dishes.common.po.BasePO;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 厨房
 * @author 好市民
 */
@Data
@Accessors(chain = true)
@TableName(value = "kitchen", autoResultMap = true)
public class KitchenEntity extends BasePO {
    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    /**
     * 厨房名称
     */
    private String name;
    /**
     * 厨房图标（预设标识）
     */
    private String cover;
    /**
     * 最大分类数
     */
    private Integer maxCategoryCount;
    /**
     * 最大菜谱数
     */
    private Integer maxDishCount;
    /**
     * 最大成员数
     */
    private Integer maxMemberCount;
    /**
     * 创建者（厨师长）用户ID
     */
    private Long ownerId;
}