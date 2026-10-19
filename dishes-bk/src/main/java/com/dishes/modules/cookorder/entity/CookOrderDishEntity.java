package com.dishes.modules.cookorder.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.dishes.common.po.BasePO;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 烹饪订单菜品关联
 * @author 好市民
 */
@Data
@Accessors(chain = true)
@TableName(value = "cook_order__dish", autoResultMap = true)
public class CookOrderDishEntity extends BasePO {
    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    /**
     * 烹饪订单ID
     */
    private Long cookOrderId;
    /**
     * 菜品ID
     */
    private Long dishId;
    /**
     * 菜品名称快照
     */
    private String dishName;
    /**
     * 菜品封面快照
     */
    private String dishCover;
    /**
     * 数量
     */
    private Integer quantity;
}
