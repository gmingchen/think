package com.dishes.modules.kitchen.model.dto;

import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 * 厨房删除DTO
 * @author 好市民
 */
@Data
public class KitchenDeleteDTO {
    /**
     * 厨房ID
     */
    @NotNull(message = "厨房ID不能为空")
    private Long id;
}
