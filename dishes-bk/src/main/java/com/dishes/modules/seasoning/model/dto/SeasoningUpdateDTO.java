package com.dishes.modules.seasoning.model.dto;

import lombok.Data;
import lombok.experimental.Accessors;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 调料更新DTO
 * @author 好市民
 */
@Data
@Accessors(chain = true)
public class SeasoningUpdateDTO {
    /**
     * 调料ID
     */
    @NotNull(message = "调料ID不能为空")
    private Long id;
    /**
     * 调料名称
     */
    @NotBlank(message = "调料名称不能为空")
    private String name;
}
