package com.think.modules.review.model.dto;

import com.think.common.pojo.PageParam;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotNull;

/**
 * 评议分页查询参数（按决策单查询）
 *
 * @author 好市民
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ReviewPageDTO extends PageParam {
    /**
     * 决策单ID
     */
    @NotNull(message = "决策单ID不能为空")
    private Long decisionId;
}
