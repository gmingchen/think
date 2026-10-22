package com.think.modules.grp.model.dto;

import com.think.common.pojo.PageParam;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.validation.constraints.NotNull;

/**
 * 群成员分页查询参数
 *
 * @author 好市民
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class GrpMemberPageDTO extends PageParam {
    /**
     * 群ID
     */
    @NotNull(message = "群ID不能为空")
    private Long grpId;
}
