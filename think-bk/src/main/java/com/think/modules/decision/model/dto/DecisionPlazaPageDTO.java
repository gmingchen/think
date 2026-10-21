package com.think.modules.decision.model.dto;

import com.think.common.pojo.PageParam;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 公开广场分页参数（可选按标签筛选）
 *
 * @author 好市民
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class DecisionPlazaPageDTO extends PageParam {
    /**
     * 标签ID过滤（可选）
     */
    private Long tagId;
}
