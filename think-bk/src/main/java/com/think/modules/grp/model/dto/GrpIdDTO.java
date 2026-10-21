package com.think.modules.grp.model.dto;

import lombok.Data;
import lombok.experimental.Accessors;

import javax.validation.constraints.NotNull;

/**
 * 群ID参数（详情 / 解散 / 邀请 / 退出 复用）
 *
 * @author 好市民
 */
@Data
@Accessors(chain = true)
public class GrpIdDTO {
    /**
     * 群ID
     */
    @NotNull(message = "群ID不能为空")
    private Long grpId;
}
