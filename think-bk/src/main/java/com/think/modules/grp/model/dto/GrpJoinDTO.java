package com.think.modules.grp.model.dto;

import lombok.Data;
import lombok.experimental.Accessors;

import javax.validation.constraints.NotBlank;

/**
 * 邀请码参数（落地页凭 code 入群）
 *
 * @author 好市民
 */
@Data
@Accessors(chain = true)
public class GrpJoinDTO {
    /**
     * 邀请码（服务端结合 Redis 生成的短时效 code）
     */
    @NotBlank(message = "邀请码不能为空")
    private String code;
}
