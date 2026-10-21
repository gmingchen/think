package com.think.modules.decision.model.dto;

import com.think.common.pojo.PageParam;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 决策单分页查询参数
 *
 * @author 好市民
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class DecisionPageDTO extends PageParam {
    /**
     * 状态过滤（可选，DecisionStatusEnum）
     */
    private Integer status;
    /**
     * 可见范围过滤（可选，VisibilityEnum）
     */
    private Integer visibility;
}
