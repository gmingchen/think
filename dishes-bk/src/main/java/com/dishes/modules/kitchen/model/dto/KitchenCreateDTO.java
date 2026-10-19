package com.dishes.modules.kitchen.model.dto;

import lombok.Data;
import lombok.experimental.Accessors;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 厨房创建DTO
 * @author 好市民
 */
@Data
@Accessors(chain = true)
public class KitchenCreateDTO {
    /**
     * 用户ID
     */
    private Long userId;
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
