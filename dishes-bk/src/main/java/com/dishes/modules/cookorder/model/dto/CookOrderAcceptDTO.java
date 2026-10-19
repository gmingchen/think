package com.dishes.modules.cookorder.model.dto;

import lombok.Data;
import lombok.experimental.Accessors;

import javax.validation.constraints.NotNull;

/**
 * 烹饪订单接单DTO
 * @author 好市民
 */
@Data
@Accessors(chain = true)
public class CookOrderAcceptDTO {
    /**
     * 订单ID
     */
    @NotNull(message = "订单ID不能为空")
    private Long id;
}
