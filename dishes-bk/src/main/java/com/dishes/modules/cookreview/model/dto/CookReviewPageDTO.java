package com.dishes.modules.cookreview.model.dto;

import com.dishes.common.pojo.PageParam;
import lombok.Data;
import lombok.experimental.Accessors;

import javax.validation.constraints.NotNull;

/**
 * 烹饪评价分页查询DTO
 * @author 好市民
 */
@Data
@Accessors(chain = true)
public class CookReviewPageDTO extends PageParam {
    /**
     * 菜谱ID
     */
    @NotNull(message = "菜谱ID不能为空")
    private Long dishId;
}
