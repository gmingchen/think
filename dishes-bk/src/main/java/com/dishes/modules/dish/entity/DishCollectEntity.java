package com.dishes.modules.dish.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.dishes.common.po.BasePO;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 菜谱收藏记录
 * @author 好市民
 */
@Data
@Accessors(chain = true)
@TableName(value = "dish_collect", autoResultMap = true)
public class DishCollectEntity extends BasePO {
    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    /**
     * 源菜谱ID（被收藏的菜谱）
     */
    private Long dishId;
    /**
     * 目标厨房ID（收藏到哪个厨房）
     */
    private Long kitchenId;
    /**
     * 执行收藏操作的用户ID
     */
    private Long userId;
    /**
     * 收藏后在目标厨房生成的新菜谱ID
     */
    private Long targetDishId;
}
