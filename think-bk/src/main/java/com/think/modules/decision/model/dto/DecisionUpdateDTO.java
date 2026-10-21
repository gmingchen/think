package com.think.modules.decision.model.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.experimental.Accessors;

import javax.validation.constraints.NotNull;

/**
 * 编辑决策单参数（仅草稿态/审核退回态可改：status=0 且非待审核）
 *
 * @author 好市民
 */
@Data
@Accessors(chain = true)
@EqualsAndHashCode(callSuper = true)
public class DecisionUpdateDTO extends DecisionCreateDTO {
    /**
     * 决策单ID
     */
    @NotNull(message = "决策单ID不能为空")
    private Long id;
}
