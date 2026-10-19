package com.dishes.modules.kitchen.model.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * 厨房成员删除DTO
 * @author 好市民
 */
@Data
public class KitchenUserDeleteDTO {
    /**
     * 厨房ID
     */
    @NotNull(message = "厨房ID不能为空")
    private Long kitchenId;
    /**
     * 用户ID
     */
    @NotNull(message = "用户ID不能为空")
    private Long userId;
}
