package com.dishes.modules.seasoning.model.dto;

import lombok.Data;
import lombok.experimental.Accessors;

import javax.validation.constraints.NotNull;

/**
 * 调料删除DTO
 * @author 好市民
 */
@Data
@Accessors(chain = true)
public class SeasoningDeleteDTO {
    /**
     * 调料ID
     */
    @NotNull(message = "调料ID不能为空")
    private Long id;
}
