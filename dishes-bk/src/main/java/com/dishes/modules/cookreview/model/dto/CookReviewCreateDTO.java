package com.dishes.modules.cookreview.model.dto;

import lombok.Data;
import lombok.experimental.Accessors;

import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import java.util.List;

/**
 * 烹饪评价创建DTO
 * @author 好市民
 */
@Data
@Accessors(chain = true)
public class CookReviewCreateDTO {
    /**
     * 烹饪订单ID
     */
    @NotNull(message = "订单ID不能为空")
    private Long cookOrderId;
    /**
     * 菜谱评价列表
     */
    @NotEmpty(message = "菜谱评价不能为空")
    private List<CookReviewDishDTO> dishes;
}
