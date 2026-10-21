package com.think.modules.grp.model.dto;

import lombok.Data;
import lombok.experimental.Accessors;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * 修改群信息参数（仅群主）
 *
 * @author 好市民
 */
@Data
@Accessors(chain = true)
public class GrpUpdateDTO {
    /**
     * 群ID
     */
    @NotNull(message = "群ID不能为空")
    private Long grpId;
    /**
     * 群名称
     */
    @NotBlank(message = "群名称不能为空")
    @Size(max = 64, message = "群名称过长")
    private String name;
    /**
     * 群头像（预设标识，可空）
     */
    private String avatar;
}
