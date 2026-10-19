package com.dishes.modules.kitchen.model.dto;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 厨房用户新增DTO
 * @author 好市民
 */
@Data
@Accessors(chain = true)
public class KitchenUserCreateDTO {
    /**
     * 厨房ID
     */
    private Long kitchenId;
    /**
     * 用户ID
     */
    private Long userId;
    /**
     * 角色
     */
    private Integer role;
    /**
     * 邀请人用户ID
     */
    private Long inviterId;
}
