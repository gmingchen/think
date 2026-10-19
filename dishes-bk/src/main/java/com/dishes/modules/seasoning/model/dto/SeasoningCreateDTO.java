package com.dishes.modules.seasoning.model.dto;

import lombok.Data;
import lombok.experimental.Accessors;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 调料创建DTO
 * @author 好市民
 */
@Data
@Accessors(chain = true)
public class SeasoningCreateDTO {
    /**
     * 厨房ID
     */
    @NotNull(message = "厨房ID不能为空")
    private Long kitchenId;
    /**
     * 调料名称
     */
    @NotBlank(message = "调料名称不能为空")
    private String name;
}
