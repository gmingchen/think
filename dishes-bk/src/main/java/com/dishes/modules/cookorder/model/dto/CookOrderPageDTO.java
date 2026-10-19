package com.dishes.modules.cookorder.model.dto;

import com.dishes.common.pojo.PageParam;
import lombok.Data;
import lombok.experimental.Accessors;

import javax.validation.constraints.NotNull;

/**
 * 烹饪订单分页查询DTO
 * @author 好市民
 */
@Data
@Accessors(chain = true)
public class CookOrderPageDTO extends PageParam {
    /**
     * 厨房ID
     */
    @NotNull(message = "厨房ID不能为空")
    private Long kitchenId;
    /**
     * 类型：1-我下的订单 2-收到的订单（不传则全部）
     */
    private Integer type;
}
