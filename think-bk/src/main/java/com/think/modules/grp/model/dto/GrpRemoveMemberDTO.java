package com.think.modules.grp.model.dto;

import lombok.Data;
import lombok.experimental.Accessors;

import javax.validation.constraints.NotNull;

/**
 * 移除群成员参数（仅群主）
 *
 * @author 好市民
 */
@Data
@Accessors(chain = true)
public class GrpRemoveMemberDTO {
    /**
     * 群ID
     */
    @NotNull(message = "群ID不能为空")
    private Long grpId;
    /**
     * 被移除成员用户ID
     */
    @NotNull(message = "成员ID不能为空")
    private Long memberId;
}
