package com.dishes.modules.kitchen.model.dto;

import lombok.Data;
import lombok.experimental.Accessors;

/**
 * 厨房用户修改角色DTO
 * @author 好市民
 */
@Data
@Accessors(chain = true)
public class KitchenUserUpdateRoleDTO {
    /**
     * 厨房ID
     */
    private Long kitchenId;
    /**
     * 用户ID
     */
    private Long userId;
    /**
     * 新角色
     */
    private Integer role;
}
