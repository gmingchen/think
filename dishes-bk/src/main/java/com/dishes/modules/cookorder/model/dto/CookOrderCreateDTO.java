package com.dishes.modules.cookorder.model.dto;

import lombok.Data;
import lombok.experimental.Accessors;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;

/**
 * 烹饪订单创建DTO
 * @author 好市民
 */
@Data
@Accessors(chain = true)
public class CookOrderCreateDTO {
    /**
     * 厨房ID
     */
    @NotNull(message = "厨房ID不能为空")
    private Long kitchenId;
    /**
     * 订单日期
     */
    @NotNull(message = "订单日期不能为空")
    private LocalDate date;
    /**
     * 时段：1-早餐 2-午餐 3-晚餐 4-宵夜
     */
    @NotNull(message = "时段不能为空")
    private Integer timePeriod;
    /**
     * 备注
     */
    private String remark;
    /**
     * 菜谱列表
     */
    @Valid
    @NotEmpty(message = "菜谱列表不能为空")
    private List<CookOrderDishDTO> dishes;
}
