package com.dishes.modules.dish.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.dishes.common.po.BasePO;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 菜谱步骤
 * @author 好市民
 */
@Data
@Accessors(chain = true)
@TableName(value = "dish_step", autoResultMap = true)
public class DishStepEntity extends BasePO {
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
     * 步骤描述
     */
    @TableField("`desc`")
    private String description;
    /**
     * 步骤图URL
     */
    private String image;
    /**
     * 排序值
     */
    private Integer sort;
}
