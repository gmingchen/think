package com.dishes.modules.kitchen.entity;

import com.baomidou.mybatisplus.annotation.*;
import com.dishes.common.po.BasePO;
import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 厨房用户关联
 * @author 好市民
 */
@Data
@Accessors(chain = true)
@TableName(value = "kitchen__user", autoResultMap = true)
public class KitchenUserEntity extends BasePO {
    /**
     * 主键
     */
    @TableId(value = "id", type = IdType.AUTO)
    private Long id;
    /**
     * 厨房ID
     */
    private Long kitchenId;
    /**
     * 用户ID
     */
    private Long userId;
    /**
     * 角色：1-厨师长 2-管厨 3-食客
     */
    private Integer role;
    /**
     * 邀请人用户ID
     */
    private Long inviterId;
}
