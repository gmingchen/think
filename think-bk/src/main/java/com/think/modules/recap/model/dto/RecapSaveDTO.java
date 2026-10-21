package com.think.modules.recap.model.dto;

import lombok.Data;
import lombok.experimental.Accessors;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * 复盘回填参数（发起人记录最终选择与实际结局，§3.5）
 *
 * @author 好市民
 */
@Data
@Accessors(chain = true)
public class RecapSaveDTO {
    /**
     * 决策单ID
     */
    @NotNull(message = "决策单ID不能为空")
    private Long decisionId;
    /**
     * 最终选择：options 下标（0-based）；-1=其他/未选
     */
    @NotNull(message = "请选择最终选择")
    private Integer finalChoice;
    /**
     * 实际发生了什么（可选）
     */
    @Size(max = 2000, message = "内容过长")
    private String actualResult;
}
