package com.dishes.modules.cookorder.model.dto;

import lombok.Data;
import lombok.experimental.Accessors;

import javax.validation.Valid;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.time.LocalDate;
import java.util.List;

/**
 * 烹饪订单更新DTO
 * @author 好市民
 */
@Data
@Accessors(chain = true)
public class CookOrderUpdateDTO {
    /**
     * 订单ID
     */
    @NotNull(message = "订单ID不能为空")
    private Long id;
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
     * 菜谱列表（传则全量替换）
     */
    @Valid
    @NotEmpty(message = "菜谱不能为空")
    private List<CookOrderDishDTO> dishes;
}
