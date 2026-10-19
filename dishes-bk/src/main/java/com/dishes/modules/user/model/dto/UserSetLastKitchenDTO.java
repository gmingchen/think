package com.dishes.modules.user.model.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * 设置上次选择的厨房ID
 * @author 好市民
 */
@Data
public class UserSetLastKitchenDTO {
    /**
     * 厨房ID
     */
    @NotNull(message = "厨房ID不能为空")
    private Long kitchenId;
}
