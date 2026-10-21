package com.think.modules.decision.model.dto;

import com.think.common.pojo.PageParam;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotNull;

/**
 * 群内决策单列表分页参数（按群 + 状态筛选）
 *
 * @author 好市民
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class DecisionGrpPageDTO extends PageParam {
    /**
     * 群ID
     */
    @NotNull(message = "群ID不能为空")
    private Long grpId;
    /**
     * 状态过滤（可选，DecisionStatusEnum）
     */
    private Integer status;
}
