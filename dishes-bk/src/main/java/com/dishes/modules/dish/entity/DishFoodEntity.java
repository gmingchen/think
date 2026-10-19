package com.dishes.modules.dish.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.dishes.common.po.BasePO;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 菜谱食材关联
 * @author 好市民
 */
@Data
@Accessors(chain = true)
@TableName(value = "dish__food", autoResultMap = true)
public class DishFoodEntity extends BasePO {
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
     * 食材ID
     */
    private Long foodId;
    /**
     * 用量
     */
    private String amount;
    /**
     * 单位
     */
    private String unit;
    /**
     * 处理方式
     */
    private String process;
    /**
     * 是否可选
     */
    private Integer isOptional;
    /**
     * 排序值
     */
    private Integer sort;
}
