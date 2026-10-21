package com.think.modules.grp.model.dto;

import com.think.common.pojo.PageParam;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 群组分页查询参数
 *
 * @author 好市民
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class GrpPageDTO extends PageParam {
    /**
     * 群名称关键字（可选）
     */
    private String keyword;
}
