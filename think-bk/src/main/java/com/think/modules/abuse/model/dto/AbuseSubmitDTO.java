package com.think.modules.abuse.model.dto;

import com.think.common.enums.AbuseTargetTypeEnum;
import com.think.core.validator.constraints.Enum;
import lombok.Data;
import lombok.experimental.Accessors;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

/**
 * 举报提交参数
 *
 * @author 好市民
 */
@Data
@Accessors(chain = true)
public class AbuseSubmitDTO {
    /**
     * 举报对象类型：1-决策单 2-评议
     */
    @NotNull(message = "举报对象类型不能为空")
    @Enum(value = AbuseTargetTypeEnum.class, message = "举报对象类型不合法")
    private Integer targetType;
    /**
     * 对象ID
     */
    @NotNull(message = "举报对象ID不能为空")
    private Long targetId;
    /**
     * 举报理由
     */
    @NotBlank(message = "举报理由不能为空")
    private String reason;
}
