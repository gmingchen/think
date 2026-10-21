package com.think.modules.decision.model.dto;

import lombok.Data;
import lombok.experimental.Accessors;

import javax.validation.constraints.NotNull;

/**
 * 决策单ID参数（提前结束 / 关闭 / 删除 复用）
 *
 * @author 好市民
 */
@Data
@Accessors(chain = true)
public class DecisionIdDTO {
    /**
     * 决策单ID
     */
    @NotNull(message = "决策单ID不能为空")
    private Long id;
}
