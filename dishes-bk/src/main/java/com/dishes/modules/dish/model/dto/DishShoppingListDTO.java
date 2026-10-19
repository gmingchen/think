package com.dishes.modules.dish.model.dto;

import lombok.Data;
import lombok.experimental.Accessors;

import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * 采购清单批量查询DTO
 * 两个汇总入参至少传一个，可同时传合并汇总
 * @author 好市民
 */
@Data
@Accessors(chain = true)
public class DishShoppingListDTO {
    /**
     * 厨房ID（dishIds 路径的权限边界，订单路径按各自订单厨房校验）
     */
    @NotNull(message = "厨房ID不能为空")
    private Long kitchenId;
    /**
     * 菜谱ID列表（重复出现表示份数，按出现次数计数）
     */
    private List<Long> dishIds;
    /**
     * 烹饪订单ID列表（份数取 cook_order__dish.quantity 累加）
     */
    private List<Long> cookOrderIds;
}
