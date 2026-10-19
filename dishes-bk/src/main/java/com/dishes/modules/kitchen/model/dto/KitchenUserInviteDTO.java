package com.dishes.modules.kitchen.model.dto;

import lombok.Data;
import lombok.experimental.Accessors;

import javax.validation.constraints.NotNull;

/**
 * 厨房邀请DTO
 * @author 好市民
 */
@Data
@Accessors(chain = true)
public class KitchenUserInviteDTO {
    /**
     * 厨房ID
     */
    @NotNull(message = "厨房ID不能为空")
    private Long kitchenId;
    /**
     * 邀请人用户ID（内部设置，无需传入）
     */
    private Long inviterId;
    /**
     * 被邀请人角色
     */
    @NotNull(message = "角色不能为空")
    private Integer role;
}
