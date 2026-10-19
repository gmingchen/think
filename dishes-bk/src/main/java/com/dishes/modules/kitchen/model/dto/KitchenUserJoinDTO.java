package com.dishes.modules.kitchen.model.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 加入厨房DTO
 * @author 好市民
 */
@Data
public class KitchenUserJoinDTO {
    /**
     * 厨房ID
     */
    @NotNull(message = "厨房ID不能为空")
    private Long kitchenId;
    /**
     * 邀请码
     */
    @NotBlank(message = "邀请码不能为空")
    private String code;
}
