package com.dishes.modules.kitchen.model.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 厨房更新DTO
 * @author 好市民
 */
@Data
public class KitchenUpdateDTO {
    /**
     * 厨房ID
     */
    @NotNull(message = "厨房ID不能为空")
    private Long id;
    /**
     * 厨房名称
     */
    @NotBlank(message = "厨房名称不能为空")
    private String name;
    /**
     * 厨房图标
     */
    private String cover;
}
