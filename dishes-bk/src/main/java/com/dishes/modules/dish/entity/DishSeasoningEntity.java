package com.dishes.modules.dish.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.dishes.common.po.BasePO;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 菜谱调料关联
 * @author 好市民
 */
@Data
@Accessors(chain = true)
@TableName(value = "dish__seasoning", autoResultMap = true)
public class DishSeasoningEntity extends BasePO {
    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    /**
     * 菜谱ID
     */
    private Long dishId;
    /**
     * 调料ID
     */
    private Long seasoningId;
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
